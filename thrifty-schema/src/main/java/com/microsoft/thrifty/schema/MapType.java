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
 * Represents a Thrift <code>map&lt;K, V&gt;</code>.
 */
public class MapType extends ThriftType {
    private final ThriftType keyType;
    private final ThriftType valueType;
    private final ImmutableMap<String, String> annotations;

    MapType(ThriftType keyType, ThriftType valueType) {
        this(keyType, valueType, ImmutableMap.<String, String>of());
    }

    MapType(ThriftType keyType, ThriftType valueType, ImmutableMap<String, String> annotations) {
        super("map<" + keyType.name() + ", " + valueType.name() + ">");
        this.keyType = keyType;
        this.valueType = valueType;
        this.annotations = annotations;
    }

    public ThriftType keyType() {
        return keyType;
    }

    public ThriftType valueType() {
        return valueType;
    }

    @Override
    public boolean isMap() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitMap(this);
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    @Override
    public ThriftType withAnnotations(Map<String, String> annotations) {
        return new MapType(keyType, valueType, merge(this.annotations, annotations));
    }
}
