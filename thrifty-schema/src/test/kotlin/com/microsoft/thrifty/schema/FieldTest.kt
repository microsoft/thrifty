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
package com.microsoft.thrifty.schema

import com.google.common.collect.ImmutableMap
import com.microsoft.thrifty.schema.parser.AnnotationElement
import com.microsoft.thrifty.schema.parser.FieldElement
import com.microsoft.thrifty.schema.parser.TypeElement
import org.junit.Ignore
import org.junit.Test

import java.util.Collections

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock

class FieldTest {
    private var location: Location = Location.get("", "")
    private var fieldId: Int = 1
    private var fieldName: String = "foo"
    private var fieldType: TypeElement = TypeElement.scalar(location, "i32", null)
    private var requiredness: Requiredness = Requiredness.DEFAULT
    private var annotations: AnnotationElement? = null
    private var documentation: String = ""

    @Test
    fun requiredFields() {
        requiredness = Requiredness.REQUIRED
        val element = field()

        val field = Field(element)
        assertTrue(field.required)
        assertFalse(field.optional)
    }

    @Test
    fun optionalFields() {
        requiredness = Requiredness.OPTIONAL
        val element = field()
        val field = Field(element)
        assertFalse(field.required)
        assertTrue(field.optional)
    }

    @Test
    fun defaultFields() {
        val element = field()
        val field = Field(element)
        assertFalse(field.required)
        assertFalse(field.optional)
    }

    @Test
    fun unredactedAndUnobfuscatedByDefault() {
        val element = field()
        val field = Field(element)
        assertFalse(field.isRedacted)
        assertFalse(field.isObfuscated)
    }

    @Test
    fun redactedByThriftAnnotation() {
        annotations = annotation("thrifty.redacted")
        val element = field()

        val field = Field(element)
        assertTrue(field.isRedacted)
    }

    @Test
    fun redactedByShortThriftAnnotation() {
        annotations = annotation("redacted")
        val element = field()

        val field = Field(element)
        assertTrue(field.isRedacted)
    }

    @Test
    fun redactedByJavadocAnnotation() {
        documentation = "/** @redacted */"
        val element = field()

        val field = Field(element)
        assertTrue(field.isRedacted)
    }

    @Test
    fun obfuscatedByThriftAnnotation() {
        annotations = annotation("thrifty.obfuscated")
        val element = field()

        val field = Field(element)
        assertTrue(field.isObfuscated)
    }

    @Test
    fun obfuscatedByShortThriftAnnotation() {
        annotations = annotation("obfuscated")
        val element = field()

        val field = Field(element)
        assertTrue(field.isObfuscated)
    }

    @Test
    fun obfuscatedByJavadocAnnotation() {
        documentation = "/** @obfuscated */"
        val element = field()

        val field = Field(element)
        assertTrue(field.isObfuscated)
    }

    @Test
    fun builderCreatesCorrectField() {
        val fieldElement = field()
        val field = Field(fieldElement)

        val annotations = ImmutableMap.of<String, String>()
        val thriftType = mock(ThriftType::class.java)

        val builderField = field.toBuilder()
                .annotations(annotations)
                .type(thriftType)
                .build()

        assertEquals(builderField.annotations, annotations)
        assertEquals(builderField.type, thriftType)
    }

    @Test
    @Ignore
    fun toBuilderCreatesCorrectField() {
        val fieldElement = mock(FieldElement::class.java)
        val field = Field(fieldElement)

        assertEquals(field.toBuilder().build(), field)
    }

    private fun annotation(name: String): AnnotationElement {
        return AnnotationElement(Location.get("", ""), Collections.singletonMap(name, "true"))
    }

    private fun field(): FieldElement {
        return FieldElement(
                location = location,
                fieldId = fieldId,
                type = fieldType,
                name = fieldName,
                requiredness = requiredness,
                documentation = documentation,
                constValue = null,
                annotations = annotations)
    }
}