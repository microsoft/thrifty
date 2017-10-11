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
package com.microsoft.thrifty.schema;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.ConstValueElement;
import com.microsoft.thrifty.schema.parser.FieldElement;

import java.util.UUID;

import javax.annotation.Nullable;

public class Field implements UserElement {
    private final FieldElement element;
    private final UserElementMixin mixin;

    private ThriftType type;

    Field(FieldElement element) {
        this.element = element;
        this.mixin = new UserElementMixin(element);
    }

    private Field(Builder builder) {
        this.mixin = builder.mixin;
        this.element = builder.fieldElement;
        this.type = builder.fieldType;
    }

    public ThriftType type() {
        return type;
    }

    public int id() {
        return element.fieldId();
    }

    public boolean optional() {
        return element.requiredness() == Requiredness.OPTIONAL;
    }

    public boolean required() {
        return element.requiredness() == Requiredness.REQUIRED;
    }

    @Nullable
    public ConstValueElement defaultValue() {
        return element.constValue();
    }

    public boolean isRedacted() {
        return mixin.hasThriftOrJavadocAnnotation("redacted");
    }

    public boolean isObfuscated() {
        return mixin.hasThriftOrJavadocAnnotation("obfuscated");
    }

    @Nullable
    public String typedefName() {
        String name = null;
        if (type != null && type.isTypedef()) {
            name = type.name();
        }
        return name;
    }

    @Override
    public UUID uuid() {
        return mixin.uuid();
    }

    @Override
    public String name() {
        return mixin.name();
    }

    @Override
    public Location location() {
        return mixin.location();
    }

    @Override
    public String documentation() {
        return mixin.documentation();
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return mixin.annotations();
    }

    @Override
    public boolean hasJavadoc() {
        return mixin.hasJavadoc();
    }

    @Override
    public boolean isDeprecated() {
        return mixin.isDeprecated();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    void link(Linker linker) {
        this.type = linker.resolveType(element.type());
    }

    void validate(Linker linker) {
        ConstValueElement value = element.constValue();
        if (value != null) {
            try {
                Constant.validate(linker, value, type);
            } catch (IllegalStateException e) {
                linker.addError(value.location(), e.getMessage());
            }
        }
    }

    public static final class Builder extends AbstractUserElementBuilder<Field, Builder> {
        private FieldElement fieldElement;
        private ThriftType fieldType;

        Builder(Field field) {
            super(field.mixin);
            this.fieldElement = field.element;
            this.fieldType = field.type;
        }

        public Builder type(ThriftType type) {
            this.fieldType = Preconditions.checkNotNull(type, "type");
            return this;
        }

        @Override
        public Field build() {
            return new Field(this);
        }
    }
}
