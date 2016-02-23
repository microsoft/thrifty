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
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.Location;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class AnnotationElement {
    public abstract Location location();
    public abstract ImmutableMap<String, String> values();

    @Nullable
    public String get(@Nonnull String name) {
        return values().get(name);
    }

    public boolean containsKey(@Nonnull String name) {
        return values().containsKey(name);
    }

    public boolean isEmpty() {
        return values().isEmpty();
    }

    public int size() {
        return values().size();
    }

    public static AnnotationElement create(Location location, Map<String, String> values) {
        return new AutoValue_AnnotationElement(location, ImmutableMap.copyOf(values));
    }
}
