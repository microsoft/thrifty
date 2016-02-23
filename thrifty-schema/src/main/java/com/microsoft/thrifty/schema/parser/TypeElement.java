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

import com.microsoft.thrifty.schema.Location;

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
