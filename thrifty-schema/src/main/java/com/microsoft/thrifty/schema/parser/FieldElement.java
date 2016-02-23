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
import com.microsoft.thrifty.schema.Requiredness;

import javax.annotation.Nullable;

@AutoValue
public abstract class FieldElement {
    public static Builder builder(Location location) {
        return new AutoValue_FieldElement.Builder()
                .location(location)
                .documentation("")
                .requiredness(Requiredness.DEFAULT);
    }

    public FieldElement withId(int fieldId) {
        return new AutoValue_FieldElement.Builder(this)
                .fieldId(fieldId)
                .build();
    }

    public abstract Location location();
    public abstract String documentation();
    @Nullable public abstract Integer fieldId();
    public abstract Requiredness requiredness();
    public abstract TypeElement type();
    public abstract String name();
    @Nullable public abstract ConstValueElement constValue();
    @Nullable public abstract AnnotationElement annotations();

    FieldElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder fieldId(Integer fieldId);
        Builder requiredness(Requiredness requiredness);
        Builder type(TypeElement type);
        Builder name(String name);
        Builder constValue(ConstValueElement constValue);
        Builder annotations(AnnotationElement annotations);

        FieldElement build();
    }
}
