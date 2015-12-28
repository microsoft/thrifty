/*
 * Copyright (C) 2015 Benjamin Bader
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

import javax.annotation.Nullable;

@AutoValue
public abstract class FieldElement {
    public static Builder builder(Location location) {
        return new AutoValue_FieldElement.Builder()
                .location(location)
                .documentation("")
                .required(false);
    }

    public FieldElement withId(int fieldId) {
        return new AutoValue_FieldElement.Builder(this)
                .fieldId(fieldId)
                .build();
    }

    public abstract Location location();
    public abstract String documentation();
    @Nullable public abstract Integer fieldId();
    public abstract boolean required();
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
        Builder required(boolean required);
        Builder type(TypeElement type);
        Builder name(String name);
        Builder constValue(ConstValueElement constValue);
        Builder annotations(AnnotationElement annotations);

        FieldElement build();
    }
}
