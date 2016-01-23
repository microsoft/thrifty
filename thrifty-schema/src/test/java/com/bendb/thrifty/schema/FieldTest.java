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
package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.FieldElement;
import com.bendb.thrifty.schema.parser.TypeElement;
import org.junit.Test;

import static org.junit.Assert.*;

public class FieldTest {
    @Test
    public void requiredFields() {
        FieldElement element = fieldBuilder()
                .requiredness(Requiredness.REQUIRED)
                .build();
        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertTrue(field.required());
        assertFalse(field.optional());
    }

    @Test
    public void optionalFields() {
        FieldElement element = fieldBuilder()
                .requiredness(Requiredness.OPTIONAL)
                .build();
        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertFalse(field.required());
        assertTrue(field.optional());
    }

    @Test
    public void defaultFields() {
        FieldElement element = fieldBuilder()
                .requiredness(Requiredness.DEFAULT)
                .build();
        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertFalse(field.required());
        assertFalse(field.optional());
    }

    private FieldElement.Builder fieldBuilder() {
        Location location = Location.get("", "");
        return FieldElement.builder(location)
                .fieldId(1)
                .name("foo")
                .type(TypeElement.scalar(location, "i32", null));
    }
}