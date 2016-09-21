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
package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.TypeElement;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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

    @Test
    public void unredactedAndUnobfuscatedByDefault() {
        FieldElement element = fieldBuilder().build();
        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertFalse(field.isRedacted());
        assertFalse(field.isObfuscated());
    }

    @Test
    public void redactedByThriftAnnotation() {
        FieldElement element = fieldBuilder()
                .annotations(annotation("thrifty.redacted"))
                .build();

        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertTrue(field.isRedacted());
    }

    @Test
    public void redactedByShortThriftAnnotation() {
        FieldElement element = fieldBuilder()
                .annotations(annotation("redacted"))
                .build();

        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertTrue(field.isRedacted());
    }

    @Test
    public void redactedByJavadocAnnotation() {
        FieldElement element = fieldBuilder()
                .documentation("/** @redacted */")
                .build();

        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertTrue(field.isRedacted());
    }

    @Test
    public void obfuscatedByThriftAnnotation() {
        FieldElement element = fieldBuilder()
                .annotations(annotation("thrifty.obfuscated"))
                .build();

        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertTrue(field.isObfuscated());
    }

    @Test
    public void obfuscatedByShortThriftAnnotation() {
        FieldElement element = fieldBuilder()
                .annotations(annotation("obfuscated"))
                .build();

        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertTrue(field.isObfuscated());
    }

    @Test
    public void obfuscatedByJavadocAnnotation() {
        FieldElement element = fieldBuilder()
                .documentation("/** @obfuscated */")
                .build();

        Field field = new Field(element, FieldNamingPolicy.DEFAULT);
        assertTrue(field.isObfuscated());
    }

    @Test
    public void builderCreatesCorrectField() {
        FieldElement fieldElement = mock(FieldElement.class);
        FieldNamingPolicy fieldNamingPolicy = mock(FieldNamingPolicy.class);
        Field field = new Field(fieldElement, fieldNamingPolicy);

        ImmutableMap<String, String> annotations = ImmutableMap.of();
        ThriftType thriftType = mock(ThriftType.class);

        Field builderField = field.toBuilder()
                .annotations(annotations)
                .type(thriftType)
                .build();

        assertEquals(builderField.annotations(), annotations);
        assertEquals(builderField.type(), thriftType);
    }

    @Test
    public void toBuilderCreatesCorrectField() {
        FieldElement fieldElement = mock(FieldElement.class);
        FieldNamingPolicy fieldNamingPolicy = mock(FieldNamingPolicy.class);
        Field field = new Field(fieldElement, fieldNamingPolicy);

        assertEquals(field.toBuilder().build(), field);
    }

    private AnnotationElement annotation(String name) {
        return AnnotationElement.create(Location.get("", ""), Collections.singletonMap(name, "true"));
    }

    private FieldElement.Builder fieldBuilder() {
        Location location = Location.get("", "");
        return FieldElement.builder(location)
                .fieldId(1)
                .name("foo")
                .type(TypeElement.scalar(location, "i32", null));
    }
}