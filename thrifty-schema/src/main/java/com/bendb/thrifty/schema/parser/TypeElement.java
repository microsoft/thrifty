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

import javax.annotation.Nullable;

public abstract class TypeElement {
    public abstract Location location();
    public abstract String name();

    @Nullable
    public abstract AnnotationElement annotations();

    public static TypeElement scalar(Location location, String name, AnnotationElement annotations) {
        return ScalarTypeElement.create(location, name, annotations);
    }

    public static TypeElement list(
            Location location,
            TypeElement elementType,
            AnnotationElement annotations) {
        return ListTypeElement.create(location, elementType, annotations);
    }

    public static TypeElement set(
            Location location,
            TypeElement elementType,
            AnnotationElement annotations) {
        return SetTypeElement.create(location, elementType, annotations);
    }

    public static TypeElement map(
            Location location,
            TypeElement key,
            TypeElement value,
            AnnotationElement annotations) {
        return MapTypeElement.create(location, key, value, annotations);
    }
}
