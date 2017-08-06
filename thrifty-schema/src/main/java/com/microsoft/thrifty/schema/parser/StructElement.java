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
package com.microsoft.thrifty.schema.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.Location;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Represents one of the three aggregate Thrift declarations: struct, union, or exception.
 */
@AutoValue
public abstract class StructElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract Type type();
    public abstract UUID uuid();
    public abstract String name();
    public abstract ImmutableList<FieldElement> fields();
    @Nullable
    public abstract AnnotationElement annotations();

    public static Builder builder(Location location) {
        return new AutoValue_StructElement.Builder()
                .location(location)
                .documentation("")
                .uuid(UUID.randomUUID());
    }

    StructElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder type(Type type);
        Builder uuid(UUID uuid);
        Builder name(String name);
        Builder fields(List<FieldElement> fields);
        Builder annotations(AnnotationElement annotations);

        StructElement build();
    }

    public enum Type {
        STRUCT,
        UNION,
        EXCEPTION
    }
}
