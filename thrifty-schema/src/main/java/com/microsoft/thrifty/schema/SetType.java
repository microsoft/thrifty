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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Represents a Thrift <code>set&lt;T&gt;</code>.
 */
public class SetType extends ThriftType {
    private final ThriftType elementType;
    private final ImmutableMap<String, String> annotations;

    SetType(ThriftType elementType) {
        this(elementType, ImmutableMap.<String, String>of());
    }

    SetType(ThriftType elementType, ImmutableMap<String, String> annotations) {
        super("set<" + elementType.name() + ">");
        this.elementType = elementType;
        this.annotations = annotations;
    }

    public ThriftType elementType() {
        return elementType;
    }

    @Override
    public boolean isSet() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSet(this);
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    @Override
    public ThriftType withAnnotations(Map<String, String> annotations) {
        return new SetType(elementType, merge(this.annotations, annotations));
    }
}
