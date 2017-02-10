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

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private ThriftType keyType;
        private ThriftType valueType;
        private ImmutableMap<String, String> annotations;

        public Builder(ThriftType keyType, ThriftType valueType, ImmutableMap<String, String> annotations) {
            this.keyType = Preconditions.checkNotNull(keyType, "keyType");
            this.valueType = Preconditions.checkNotNull(valueType, "valueType");
            this.annotations = Preconditions.checkNotNull(annotations, "annotations");
        }

        Builder(MapType type) {
            this.keyType = type.keyType;
            this.valueType = type.valueType;
            this.annotations = type.annotations;
        }

        public Builder keyType(ThriftType keyType) {
            this.keyType = Preconditions.checkNotNull(keyType, "keyType");
            return this;
        }

        public Builder valueType(ThriftType valueType) {
            this.valueType = Preconditions.checkNotNull(valueType, "valueType");
            return this;
        }

        public Builder annotations(ImmutableMap<String, String> annotations) {
            this.annotations = Preconditions.checkNotNull(annotations, "annotations");
            return this;
        }

        public MapType build() {
            return new MapType(keyType, valueType, annotations);
        }
    }
}
