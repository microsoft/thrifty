/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.bendb.thrifty.gen

import com.bendb.thrifty.ThriftException
import com.bendb.thrifty.schema.BuiltinType
import com.bendb.thrifty.schema.NamespaceScope
import com.bendb.thrifty.schema.ServiceMethod
import com.bendb.thrifty.schema.ServiceType
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.Locale

import javax.lang.model.element.Modifier
import java.util.concurrent.atomic.AtomicInteger

internal class ServiceBuilder(
        private val typeResolver: TypeResolver,
        private val constantBuilder: ConstantBuilder,
        private val fieldNamer: FieldNamer
) {

    fun buildServiceInterface(service: ServiceType): TypeSpec {
        val serviceSpec = TypeSpec.interfaceBuilder(service.name)
                .addModifiers(Modifier.PUBLIC)

        service.documentation.let {
            if (it.isNotEmpty()) {
                serviceSpec.addJavadoc(it)
            }
        }

        if (service.isDeprecated) {
            serviceSpec.addAnnotation(AnnotationSpec.builder(Deprecated::class.java).build())
        }

        service.extendsService?.let {
            val superType = it.trueType
            val superTypeName = typeResolver.getJavaClass(superType)
            serviceSpec.addSuperinterface(superTypeName)
        }

        for (method in service.methods) {
            val allocator = NameAllocator()
            var tag = 0

            val methodBuilder = MethodSpec.methodBuilder(method.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)

            if (method.hasJavadoc) {
                methodBuilder.addJavadoc(method.documentation)
            }

            for (field in method.parameters) {
                val fieldName = fieldNamer.getName(field)
                val name = allocator.newName(fieldName, ++tag)
                val paramType = field.type.trueType
                val paramTypeName = typeResolver.getJavaClass(paramType)

                methodBuilder.addParameter(paramTypeName, name)

            }

            val callbackName = allocator.newName("callback", ++tag)

            val returnType = method.returnType
            val returnTypeName = if (returnType == BuiltinType.VOID) {
                ClassName.get("kotlin", "Unit")
            } else {
                typeResolver.getJavaClass(returnType.trueType)
            }

            val callbackInterfaceName = ParameterizedTypeName.get(
                    TypeNames.SERVICE_CALLBACK, returnTypeName)

            methodBuilder.addParameter(callbackInterfaceName, callbackName)

            serviceSpec.addMethod(methodBuilder.build())
        }

        return serviceSpec.build()
    }

    fun buildService(service: ServiceType, serviceInterface: TypeSpec): TypeSpec {
        val packageName = service.getNamespaceFor(NamespaceScope.JAVA)
        val interfaceTypeName = ClassName.get(packageName, serviceInterface.name)
        val builder = TypeSpec.classBuilder(service.name + "Client")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceTypeName)

        val extendsServiceType = service.extendsService
        if (extendsServiceType is ServiceType) {
            val typeName = extendsServiceType.name + "Client"
            val ns = extendsServiceType.getNamespaceFor(NamespaceScope.JAVA)
            val javaClass = ClassName.get(ns, typeName)
            builder.superclass(javaClass)
        } else {
            builder.superclass(TypeNames.SERVICE_CLIENT_BASE)
        }

        builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addParameter(TypeNames.SERVICE_CLIENT_LISTENER, "listener")
                .addStatement("super(protocol, listener)")
                .build())

        for ((i, methodSpec) in serviceInterface.methodSpecs.withIndex()) {
            val serviceMethod = service.methods[i]
            val call = buildCallSpec(serviceMethod)
            builder.addType(call)

            val meth = MethodSpec.methodBuilder(methodSpec.name)
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(methodSpec.parameters)
                    .addExceptions(methodSpec.exceptions)

            val body = CodeBlock.builder()
                    .add("$[this.enqueue(new \$N(", call)

            for ((index, parameter) in methodSpec.parameters.withIndex()) {
                if (index == 0) {
                    body.add("\$N", parameter.name)
                } else {
                    body.add(", \$N", parameter.name)
                }
            }

            body.add("));\n$]")

            meth.addCode(body.build())

            builder.addMethod(meth.build())
        }

        return builder.build()
    }

    private fun buildCallSpec(method: ServiceMethod): TypeSpec {
        val name = "${method.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Call"

        val returnType = method.returnType
        val returnTypeName = if (returnType == BuiltinType.VOID) {
            ClassName.get("kotlin", "Unit")
        } else {
            typeResolver.getJavaClass(returnType.trueType)
        }

        val callbackTypeName = ParameterizedTypeName.get(TypeNames.SERVICE_CALLBACK, returnTypeName)
        val superclass = ParameterizedTypeName.get(TypeNames.SERVICE_METHOD_CALL, returnTypeName)

        val hasReturnType = returnType != BuiltinType.VOID

        val callBuilder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .superclass(superclass)

        // Set up fields
        for (field in method.parameters) {
            val javaType = typeResolver.getJavaClass(field.type.trueType)

            callBuilder.addField(FieldSpec.builder(javaType, fieldNamer.getName(field))
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build())
        }

        // Ctor
        callBuilder.addMethod(buildCallCtor(method, callbackTypeName))

        // Send
        callBuilder.addMethod(buildSendMethod(method))

        // Receive
        callBuilder.addMethod(buildReceiveMethod(method, hasReturnType))

        return callBuilder.build()
    }

    private fun buildCallCtor(method: ServiceMethod, callbackTypeName: TypeName): MethodSpec {
        val allocator = NameAllocator()
        val scope = AtomicInteger(0)
        val ctor = MethodSpec.constructorBuilder()
                .addStatement(
                        "super(\$S, \$T.\$L, callback)",
                        method.name,
                        TypeNames.TMESSAGE_TYPE,
                        if (method.oneWay) "ONEWAY" else "CALL")

        for (field in method.parameters) {
            val fieldName = fieldNamer.getName(field)
            val javaType = typeResolver.getJavaClass(field.type.trueType)

            ctor.addParameter(javaType, fieldName)

            if (field.required && field.defaultValue == null) {
                ctor.addStatement("if (\$L == null) throw new NullPointerException(\$S)", fieldName, fieldName)
                ctor.addStatement("this.$1L = $1L", fieldName)
            } else if (field.defaultValue != null) {
                ctor.beginControlFlow("if (\$L != null)", fieldName)
                ctor.addStatement("this.$1L = $1L", fieldName)
                ctor.nextControlFlow("else")

                val init = CodeBlock.builder()
                constantBuilder.generateFieldInitializer(
                        init,
                        allocator,
                        scope,
                        "this.$fieldName",
                        field.type.trueType,
                        field.defaultValue!!,
                        false)
                ctor.addCode(init.build())

                ctor.endControlFlow()
            } else {
                ctor.addStatement("this.$1L = $1L", fieldName)
            }
        }

        ctor.addParameter(callbackTypeName, "callback")

        return ctor.build()
    }

    private fun buildSendMethod(method: ServiceMethod): MethodSpec {
        val send = MethodSpec.methodBuilder("send")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addException(TypeNames.IO_EXCEPTION)

        send.addStatement("protocol.writeStructBegin(\$S)", "args")

        for (field in method.parameters) {
            val fieldName = fieldNamer.getName(field)
            val optional = !field.required
            val tt = field.type.trueType
            val typeCode = typeResolver.getTypeCode(tt)

            if (optional) {
                send.beginControlFlow("if (this.\$L != null)", fieldName)
            }

            send.addStatement("protocol.writeFieldBegin(\$S, \$L, \$T.\$L)",
                    field.name, // send the Thrift name, not the fieldNamer output
                    field.id,
                    TypeNames.TTYPE,
                    TypeNames.getTypeCodeName(typeCode))

            tt.accept(GenerateWriterVisitor(typeResolver, send, "protocol", "this", fieldName))

            send.addStatement("protocol.writeFieldEnd()")

            if (optional) {
                send.endControlFlow()
            }
        }

        send.addStatement("protocol.writeFieldStop()")
        send.addStatement("protocol.writeStructEnd()")

        return send.build()
    }

    private fun buildReceiveMethod(method: ServiceMethod, hasReturnType: Boolean): MethodSpec {
        val recv = MethodSpec.methodBuilder("receive")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addParameter(TypeNames.MESSAGE_METADATA, "metadata")
                .addException(TypeNames.EXCEPTION)

        if (hasReturnType) {
            val retTypeName = typeResolver.getJavaClass(method.returnType.trueType)
            recv.returns(retTypeName)
            recv.addStatement("\$T result = null", retTypeName)
        } else {
            recv.returns(ClassName.get("kotlin", "Unit"))
        }

        for (field in method.exceptions) {
            val fieldName = fieldNamer.getName(field)
            val exceptionTypeName = typeResolver.getJavaClass(field.type.trueType)
            recv.addStatement("\$T \$L = null", exceptionTypeName, fieldName)
        }

        recv.addStatement("protocol.readStructBegin()")
                .beginControlFlow("while (true)")
                .addStatement("\$T field = protocol.readFieldBegin()", TypeNames.FIELD_METADATA)
                .beginControlFlow("if (field.typeId == \$T.STOP)", TypeNames.TTYPE)
                .addStatement("break")
                .endControlFlow()
                .beginControlFlow("switch (field.fieldId)")

        if (hasReturnType) {
            val type = method.returnType.trueType
            recv.beginControlFlow("case 0:")

            object : GenerateReaderVisitor(typeResolver, recv, "result", type) {
                override fun useReadValue(localName: String) {
                    recv.addStatement("result = \$N", localName)
                }
            }.generate()

            recv.endControlFlow()
            recv.addStatement("break")
        }

        for (field in method.exceptions) {
            val fieldName = fieldNamer.getName(field)
            recv.beginControlFlow("case \$L:", field.id)

            object : GenerateReaderVisitor(typeResolver, recv, fieldName, field.type.trueType) {
                override fun useReadValue(localName: String) {
                    recv.addStatement("\$N = \$N", fieldName, localName)
                }
            }.generate()

            recv.endControlFlow()
            recv.addStatement("break")
        }

        recv.addStatement("default: \$T.skip(protocol, field.typeId); break", TypeNames.PROTO_UTIL)
        recv.endControlFlow() // end switch
        recv.addStatement("protocol.readFieldEnd()")
        recv.endControlFlow() // end while
        recv.addStatement("protocol.readStructEnd()")

        var isInControlFlow = false
        if (hasReturnType) {
            recv.beginControlFlow("if (result != null)")
            recv.addStatement("return result")
            isInControlFlow = true
        }

        for (field in method.exceptions) {
            val fieldName = fieldNamer.getName(field)
            if (isInControlFlow) {
                recv.nextControlFlow("else if (\$L != null)", fieldName)
            } else {
                recv.beginControlFlow("if (\$L != null)", fieldName)
                isInControlFlow = true
            }
            recv.addStatement("throw \$L", fieldName)
        }

        if (isInControlFlow) {
            recv.nextControlFlow("else")
        }

        if (hasReturnType) {
            // In this branch, no return type was received, nor were
            // any declared exceptions received.  This is a failure.
            recv.addStatement(
                    "throw new \$T(\$T.\$L, \$S)",
                    TypeNames.THRIFT_EXCEPTION,
                    TypeNames.THRIFT_EXCEPTION_KIND,
                    ThriftException.Kind.MISSING_RESULT.name,
                    "Missing result")
        } else {
            // No return is expected, and no exceptions were received.
            // Success!
            recv.addStatement("return kotlin.Unit.INSTANCE")
        }

        if (isInControlFlow) {
            recv.endControlFlow()
        }

        return recv.build()
    }
}
