/*
 * Copyright (C) 2015 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.compiler;

import com.bendb.thrifty.compiler.spi.TypeProcessor;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.io.Serializable;

/**
 * An example {@link TypeProcessor} that implements {@link Serializable}
 * on all types.
 */
public class SerializableTypeProcessor implements TypeProcessor {
    @Override
    public TypeSpec process(TypeSpec type) {
        TypeSpec.Builder builder = type.toBuilder();

        builder.addSuperinterface(Serializable.class);

        builder.addField(FieldSpec.builder(long.class, "serialVersionUID")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", -1)
                .build());

        return builder.build();
    }
}
