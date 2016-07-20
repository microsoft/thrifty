/*
 * Thrifty
 *
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
package com.microsoft.thrifty.gen;

import com.google.common.base.Strings;
import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.ThriftException;
import com.microsoft.thrifty.schema.Field;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.Service;
import com.microsoft.thrifty.schema.ServiceMethod;
import com.microsoft.thrifty.schema.ThriftType;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

final class ServiceBuilder {
    private final TypeResolver typeResolver;
    private final ConstantBuilder constantBuilder;

    ServiceBuilder(TypeResolver typeResolver, ConstantBuilder constantBuilder) {
        this.typeResolver = typeResolver;
        this.constantBuilder = constantBuilder;
    }

    TypeSpec buildServiceInterface(Service service) {
        TypeSpec.Builder serviceSpec = TypeSpec.interfaceBuilder(service.name())
                .addModifiers(Modifier.PUBLIC);

        if (!Strings.isNullOrEmpty(service.documentation())) {
            serviceSpec.addJavadoc(service.documentation());
        }

        if (service.isDeprecated()) {
            serviceSpec.addAnnotation(AnnotationSpec.builder(Deprecated.class).build());
        }

        if (service.extendsService() != null) {
            ThriftType superType = service.extendsService().getTrueType();
            TypeName superTypeName = typeResolver.getJavaClass(superType);
            serviceSpec.addSuperinterface(superTypeName);
        }

        for (ServiceMethod method : service.methods()) {
            NameAllocator allocator = new NameAllocator();
            int tag = 0;

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.name())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

            if (method.hasJavadoc()) {
                methodBuilder.addJavadoc(method.documentation());
            }

            for (Field field : method.paramTypes()) {
                String name = allocator.newName(field.name(), ++tag);
                ThriftType paramType = field.type().getTrueType();
                TypeName paramTypeName = typeResolver.getJavaClass(paramType);

                methodBuilder.addParameter(paramTypeName, name);

            }

            String callbackName = allocator.newName("callback", ++tag);

            ThriftType returnType = method.returnType().or(ThriftType.VOID);
            TypeName returnTypeName;
            if (returnType == ThriftType.VOID) {
                returnTypeName = TypeName.VOID.box();
            } else {
                returnTypeName = typeResolver.getJavaClass(returnType.getTrueType());
            }

            TypeName callbackInterfaceName = ParameterizedTypeName.get(
                    TypeNames.SERVICE_CALLBACK, returnTypeName);

            methodBuilder.addParameter(callbackInterfaceName, callbackName);

            serviceSpec.addMethod(methodBuilder.build());
        }

        return serviceSpec.build();
    }

    TypeSpec buildService(Service service, TypeSpec serviceInterface) {
        String packageName = service.getNamespaceFor(NamespaceScope.JAVA);
        TypeName interfaceTypeName = ClassName.get(packageName, serviceInterface.name);
        TypeSpec.Builder builder = TypeSpec.classBuilder(service.name() + "Client")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceTypeName);

        if (service.extendsService() != null) {
            ThriftType type = service.extendsService();
            String typeName = type.name() + "Client";
            String ns = type.getNamespace(NamespaceScope.JAVA);
            TypeName javaClass = ClassName.get(ns, typeName);
            builder.superclass(javaClass);
        } else {
            builder.superclass(TypeNames.SERVICE_CLIENT_BASE);
        }

        builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addParameter(TypeNames.SERVICE_CLIENT_LISTENER, "listener")
                .addStatement("super(protocol, listener)")
                .build());

        int i = 0;
        for (MethodSpec methodSpec : serviceInterface.methodSpecs) {
            ServiceMethod serviceMethod = service.methods().get(i++);
            TypeSpec call = buildCallSpec(serviceMethod);
            builder.addType(call);

            MethodSpec.Builder meth = MethodSpec.methodBuilder(methodSpec.name)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(methodSpec.parameters)
                    .addExceptions(methodSpec.exceptions);

            CodeBlock.Builder body = CodeBlock.builder()
                    .add("$[this.enqueue(new $N(", call);

            boolean first = true;
            for (ParameterSpec parameter : methodSpec.parameters) {
                if (first) {
                    body.add("$N", parameter.name);
                    first = false;
                } else {
                    body.add(", $N", parameter.name);
                }
            }

            body.add("));\n$]");

            meth.addCode(body.build());

            builder.addMethod(meth.build());
        }

        return builder.build();
    }

    private TypeSpec buildCallSpec(ServiceMethod method) {
        String name = method.name();
        if (Character.isLowerCase(name.charAt(0))) {
            if (name.length() > 1) {
                name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            } else {
                name = name.toUpperCase(Locale.US);
            }

            name += "Call";
        }

        ThriftType returnType = method.returnType().or(ThriftType.VOID);
        TypeName returnTypeName = returnType.equals(ThriftType.VOID)
                ? TypeName.VOID.box()
                : typeResolver.getJavaClass(returnType.getTrueType());
        TypeName callbackTypeName = ParameterizedTypeName.get(TypeNames.SERVICE_CALLBACK, returnTypeName);
        TypeName superclass = ParameterizedTypeName.get(TypeNames.SERVICE_METHOD_CALL, returnTypeName);

        boolean hasReturnType = !returnTypeName.equals(TypeName.VOID.box());

        TypeSpec.Builder callBuilder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .superclass(superclass);

        // Set up fields
        for (Field field : method.paramTypes()) {
            TypeName javaType = typeResolver.getJavaClass(field.type().getTrueType());

            callBuilder.addField(FieldSpec.builder(javaType, field.name())
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build());
        }

        // Ctor
        callBuilder.addMethod(buildCallCtor(method, callbackTypeName));

        // Send
        callBuilder.addMethod(buildSendMethod(method));

        // Receive
        callBuilder.addMethod(buildReceiveMethod(method, hasReturnType));

        return callBuilder.build();
    }

    private MethodSpec buildCallCtor(ServiceMethod method, TypeName callbackTypeName) {
        NameAllocator allocator = new NameAllocator();
        AtomicInteger scope = new AtomicInteger(0);
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                .addStatement(
                        "super($S, $T.$L, callback)",
                        method.name(),
                        TypeNames.TMESSAGE_TYPE,
                        method.oneWay() ? "ONEWAY" : "CALL");

        for (Field field : method.paramTypes()) {
            TypeName javaType = typeResolver.getJavaClass(field.type().getTrueType());

            ctor.addParameter(javaType, field.name());

            if (field.required() && field.defaultValue() == null) {
                ctor.addStatement("if ($L == null) throw new NullPointerException($S)", field.name(), field.name());
                ctor.addStatement("this.$1L = $1L", field.name());
            } else if (field.defaultValue() != null) {
                ctor.beginControlFlow("if ($L != null)", field.name());
                ctor.addStatement("this.$1L = $1L", field.name(), field.name());
                ctor.nextControlFlow("else");

                CodeBlock.Builder init = CodeBlock.builder();
                constantBuilder.generateFieldInitializer(
                        init,
                        allocator,
                        scope,
                        "this." + field.name(),
                        field.type().getTrueType(),
                        field.defaultValue(),
                        false);
                ctor.addCode(init.build());

                ctor.endControlFlow();
            } else {
                ctor.addStatement("this.$1L = $1L", field.name());
            }
        }

        ctor.addParameter(callbackTypeName, "callback");

        return ctor.build();
    }

    private MethodSpec buildSendMethod(ServiceMethod method) {
        MethodSpec.Builder send = MethodSpec.methodBuilder("send")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addException(TypeNames.IO_EXCEPTION);

        send.addStatement("protocol.writeStructBegin($S)", "args");

        for (Field field : method.paramTypes()) {
            boolean optional = !field.required();
            final ThriftType tt = field.type().getTrueType();
            byte typeCode = typeResolver.getTypeCode(tt);

            // Enums are written/read as i32
            if (typeCode == TType.ENUM) {
                typeCode = TType.I32;
            }

            if (optional) {
                send.beginControlFlow("if (this.$L != null)", field.name());
            }

            send.addStatement("protocol.writeFieldBegin($S, $L, $T.$L)",
                    field.thriftName(),
                    field.id(),
                    TypeNames.TTYPE,
                    TypeNames.getTypeCodeName(typeCode));

            tt.accept(new GenerateWriterVisitor(typeResolver, send, "protocol", "this", field));

            send.addStatement("protocol.writeFieldEnd()");

            if (optional) {
                send.endControlFlow();
            }
        }

        send.addStatement("protocol.writeFieldStop()");
        send.addStatement("protocol.writeStructEnd()");

        return send.build();
    }

    private MethodSpec buildReceiveMethod(ServiceMethod method, boolean hasReturnType) {
        final MethodSpec.Builder recv = MethodSpec.methodBuilder("receive")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addParameter(TypeNames.MESSAGE_METADATA, "metadata")
                .addException(TypeNames.EXCEPTION);

        if (hasReturnType) {
            TypeName retTypeName = typeResolver.getJavaClass(method.returnType().get().getTrueType());
            recv.returns(retTypeName);
            recv.addStatement("$T result = null", retTypeName);
        } else {
            recv.returns(TypeName.VOID.box());
        }

        for (Field field : method.exceptionTypes()) {
            TypeName exceptionTypeName = typeResolver.getJavaClass(field.type().getTrueType());
            recv.addStatement("$T $L = null", exceptionTypeName, field.name());
        }

        recv.addStatement("protocol.readStructBegin()")
                .beginControlFlow("while (true)")
                .addStatement("$T field = protocol.readFieldBegin()", TypeNames.FIELD_METADATA)
                .beginControlFlow("if (field.typeId == $T.STOP)", TypeNames.TTYPE)
                .addStatement("break")
                .endControlFlow()
                .beginControlFlow("switch (field.fieldId)");

        if (hasReturnType) {
            ThriftType type = method.returnType().get().getTrueType();
            recv.beginControlFlow("case 0:");

            new GenerateReaderVisitor(typeResolver, recv, "result", type) {
                @Override
                protected void useReadValue(String localName) {
                    recv.addStatement("result = $N", localName);
                }
            }.generate();

            recv.endControlFlow();
            recv.addStatement("break");
        }

        for (final Field field : method.exceptionTypes()) {
            recv.beginControlFlow("case $L:", field.id());

            new GenerateReaderVisitor(typeResolver, recv, field) {
                @Override
                protected void useReadValue(String localName) {
                    recv.addStatement("$N = $N", field.name(), localName);
                }
            }.generate();

            recv.endControlFlow();
            recv.addStatement("break");
        }

        recv.addStatement("default: $T.skip(protocol, field.typeId); break", TypeNames.PROTO_UTIL);
        recv.endControlFlow(); // end switch
        recv.addStatement("protocol.readFieldEnd()");
        recv.endControlFlow(); // end while
        recv.addStatement("protocol.readStructEnd()");

        boolean isInControlFlow = false;
        if (hasReturnType) {
            recv.beginControlFlow("if (result != null)");
            recv.addStatement("return result");
            isInControlFlow = true;
        }

        for (Field field : method.exceptionTypes()) {
            if (isInControlFlow) {
                recv.nextControlFlow("else if ($L != null)", field.name());
            } else {
                recv.beginControlFlow("if ($L != null)", field.name());
                isInControlFlow = true;
            }
            recv.addStatement("throw $L", field.name());
        }

        if (isInControlFlow) {
            recv.nextControlFlow("else");
        }

        if (hasReturnType) {
            // In this branch, no return type was received, nor were
            // any declared exceptions received.  This is a failure.
            recv.addStatement(
                    "throw new $T($T.$L, $S)",
                    TypeNames.THRIFT_EXCEPTION,
                    TypeNames.THRIFT_EXCEPTION_KIND,
                    ThriftException.Kind.MISSING_RESULT.name(),
                    "Missing result");
        } else {
            // No return is expected, and no exceptions were received.
            // Success!
            recv.addStatement("return null");
        }

        if (isInControlFlow) {
            recv.endControlFlow();
        }

        return recv.build();
    }
}
