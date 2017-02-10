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

import java.util.Map;

/**
 * Represents a Thrift <code>list&lt;T&gt;</code>.
 */
public class ListType extends ThriftType {
    private final ThriftType elementType;
    private final ImmutableMap<String, String> annotations;

    ListType(ThriftType elementType) {
        this(elementType, ImmutableMap.<String, String>of());
    }

    ListType(ThriftType elementType, ImmutableMap<String, String> annotations) {
        super("list<" + elementType.name() + ">");
        this.elementType = elementType;
        this.annotations = annotations;
    }

    public ThriftType elementType() {
        return elementType;
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitList(this);
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    @Override
    public ThriftType withAnnotations(Map<String, String> annotations) {
        return new ListType(elementType, merge(this.annotations, annotations));
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private ThriftType elementType;
        private ImmutableMap<String, String> annotations;

        public Builder(ThriftType elementType, ImmutableMap<String, String> annotations) {
            this.elementType = Preconditions.checkNotNull(elementType, "elementType");
            this.annotations = Preconditions.checkNotNull(annotations, "annotations");
        }

        Builder(ListType type) {
            this.elementType = type.elementType;
            this.annotations = type.annotations;
        }

        public Builder elementType(ThriftType elementType) {
            this.elementType = Preconditions.checkNotNull(elementType, "elementType");
            return this;
        }

        public Builder annotations(ImmutableMap<String, String> annotations) {
            this.annotations = Preconditions.checkNotNull(annotations, "annotations");
            return this;
        }

        public ListType build() {
            return new ListType(elementType, annotations);
        }
    }
}
