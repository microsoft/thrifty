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

import com.microsoft.thrifty.schema.Field;
import com.microsoft.thrifty.schema.ServiceMethod;
import com.microsoft.thrifty.schema.ThriftType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Modifier;

final class SyncServiceBuilder extends ServiceBuilder {

    SyncServiceBuilder(TypeResolver typeResolver, ConstantBuilder constantBuilder, FieldNamer fieldNamer) {
        super(typeResolver, constantBuilder, fieldNamer);
    }

    @Override
    String getServiceInterfaceSuffix() {
        return "Sync";
    }

    @Override
    TypeName getServiceMethodReturnType(ServiceMethod method) {
        return resolveServiceMethodReturnType(method);
    }

    @Override
    List<TypeName> getDeclaredServiceMethodExceptions(ServiceMethod method) {
        List<TypeName> exceptions = new ArrayList<>(method.exceptions().size() + 1);
        exceptions.add(TypeNames.IO_EXCEPTION);
        for (Field exception : method.exceptions()) {
            ThriftType exceptionType = exception.type().getTrueType();
            TypeName exceptionTypeName = typeResolver.getJavaClass(exceptionType);

            exceptions.add(exceptionTypeName);
        }
        return exceptions;
    }

    @Override
    List<ParameterSpec> getAdditionalServiceMethodParameters(ServiceMethod method, NameAllocator allocator, int tag) {
        return Collections.emptyList();
    }

    @Override
    String getServiceClassNameSuffix() {
        return "SyncClient";
    }

    @Override
    TypeName getDefaultServiceSuperclass() {
        return TypeNames.SERVICE_SYNC_CLIENT_BASE;
    }

    @Override
    MethodSpec buildServiceCtor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addStatement("super(protocol)")
                .build();
    }

    @Override
    CodeBlock buildServiceMethodBody(MethodSpec methodSpec, TypeSpec call) {
        CodeBlock.Builder body = CodeBlock.builder()
                .beginControlFlow("try")
                .add("$[return this.execute(new $N(", call);

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

        CodeBlock.Builder catchCode = CodeBlock.builder()
                .add("catch(");
        for (TypeName exception : methodSpec.exceptions) {
            catchCode.add("$T | ", exception);
        }
        catchCode.add("$T e)", RuntimeException.class);
        return body.nextControlFlow(catchCode.build().toString())
                .addStatement("throw e")
                .nextControlFlow("catch($T e)", TypeNames.EXCEPTION)
                .addStatement("throw new $T($S, e)", AssertionError.class, "Unexpected exception")
                .endControlFlow()
                .build();

    }

    @Override
    String getCallClassNameSuffix() {
        return "SyncCall";
    }

    @Override
    ClassName getBaseCallSuperclass() {
        return TypeNames.SERVICE_SYNC_METHOD_CALL;
    }

    @Override
    CodeBlock buildCallCtorSuperStatement(ServiceMethod method) {
        return CodeBlock.builder()
                .addStatement("super($S, $T.$L)",
                        method.name(),
                        TypeNames.TMESSAGE_TYPE,
                        method.oneWay() ? "ONEWAY" : "CALL")
                .build();
    }

    @Override
    List<ParameterSpec> getAdditionalCallCtorParameters(ServiceMethod method) {
        return Collections.emptyList();
    }
}
