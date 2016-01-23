/*
 * Copyright (C) 2015-2016 Benjamin Bader
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
package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
