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
package com.microsoft.thrifty.gen;

import com.microsoft.thrifty.schema.Field;
import com.microsoft.thrifty.schema.FieldNamingPolicy;

import java.util.LinkedHashMap;
import java.util.Map;

class FieldNamer {
    private final Map<Field, String> nameCache = new LinkedHashMap<>();
    private final FieldNamingPolicy namingPolicy;

    FieldNamer(FieldNamingPolicy namingPolicy) {
        this.namingPolicy = namingPolicy;
    }

    String getName(Field field) {
        return nameCache.computeIfAbsent(field, k -> namingPolicy.apply(field.name()));
    }
}
