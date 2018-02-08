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

import com.microsoft.thrifty.schema.ServiceMethod;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Modifier;

final class AsyncServiceBuilder extends ServiceBuilder {

    AsyncServiceBuilder(TypeResolver typeResolver, ConstantBuilder constantBuilder, FieldNamer fieldNamer) {
        super(typeResolver, constantBuilder, fieldNamer);
    }

    @Override
    String getServiceInterfaceSuffix() {
        return "";
    }

    @Override
    TypeName getServiceMethodReturnType(ServiceMethod method) {
        return TypeName.VOID;
    }

    @Override
    List<TypeName> getDeclaredServiceMethodExceptions(ServiceMethod method) {
        return Collections.emptyList();
    }

    @Override
    List<ParameterSpec> getAdditionalServiceMethodParameters(ServiceMethod method, NameAllocator allocator, int tag) {
        String callbackName = allocator.newName("callback", ++tag);
        return Collections.singletonList(getCallbackParameter(method, callbackName));
    }

    @Override
    String getServiceClassNameSuffix() {
        return "Client";
    }

    @Override
    TypeName getDefaultServiceSuperclass() {
        return TypeNames.SERVICE_ASYNC_CLIENT_BASE;
    }

    @Override
    MethodSpec buildServiceCtor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addParameter(TypeNames.SERVICE_CLIENT_LISTENER, "listener")
                .addStatement("super(protocol, listener)")
                .build();
    }

    @Override
    CodeBlock buildServiceMethodBody(MethodSpec methodSpec, TypeSpec call) {
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

        return body.add("));\n$]").build();
    }

    @Override
    String getCallClassNameSuffix() {
        return "Call";
    }

    @Override
    ClassName getBaseCallSuperclass() {
        return TypeNames.SERVICE_ASYNC_METHOD_CALL;
    }

    @Override
    CodeBlock buildCallCtorSuperStatement(ServiceMethod method) {
        return CodeBlock.builder()
                .addStatement(
                        "super($S, $T.$L, callback)",
                        method.name(),
                        TypeNames.TMESSAGE_TYPE,
                        method.oneWay() ? "ONEWAY" : "CALL")
                .build();
    }

    @Override
    List<ParameterSpec> getAdditionalCallCtorParameters(ServiceMethod method) {
        return Collections.singletonList(getCallbackParameter(method, "callback"));
    }

    private ParameterSpec getCallbackParameter(ServiceMethod method, String callbackName) {
        TypeName returnTypeName = resolveServiceMethodReturnType(method);
        TypeName callbackInterfaceName = ParameterizedTypeName.get(
                TypeNames.SERVICE_CALLBACK, returnTypeName);
        return ParameterSpec.builder(callbackInterfaceName, callbackName).build();
    }
}
