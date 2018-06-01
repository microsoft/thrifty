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

@AutoValue
public abstract class FunctionElement {
    public static Builder builder(Location location) {
        return new AutoValue_FunctionElement.Builder()
                .location(location)
                .documentation("")
                .oneWay(false)
                .params(ImmutableList.of())
                .exceptions(ImmutableList.of())
                .uuid(ThriftyParserPlugins.createUUID());
    }

    public abstract Location location();
    public abstract String documentation();
    public abstract boolean oneWay();
    public abstract TypeElement returnType();
    public abstract String name();
    public abstract UUID uuid();
    public abstract ImmutableList<FieldElement> params();
    public abstract ImmutableList<FieldElement> exceptions();
    @Nullable public abstract AnnotationElement annotations();

    FunctionElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder oneWay(boolean oneWay);
        Builder returnType(TypeElement returnType);
        Builder name(String name);
        Builder uuid(UUID uuid);
        Builder params(List<FieldElement> params);
        Builder exceptions(List<FieldElement> params);
        Builder annotations(AnnotationElement annotations);

        FunctionElement build();
    }
}
