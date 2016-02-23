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
import com.microsoft.thrifty.schema.Location;

import javax.annotation.Nullable;

@AutoValue
public abstract class EnumMemberElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String name();
    @Nullable public abstract Integer value();
    @Nullable public abstract AnnotationElement annotations();

    public static Builder builder(Location location) {
        return new AutoValue_EnumMemberElement.Builder()
                .location(location)
                .documentation("");
    }

    public EnumMemberElement withValue(Integer value) {
        return new AutoValue_EnumMemberElement.Builder(this)
                .value(value)
                .build();
    }

    EnumMemberElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder name(String name);
        Builder value(Integer value);
        Builder annotations(AnnotationElement annotations);

        EnumMemberElement build();
    }
}
