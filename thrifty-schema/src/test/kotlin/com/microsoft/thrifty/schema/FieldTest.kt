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

import com.microsoft.thrifty.schema.parser.AnnotationElement
import com.microsoft.thrifty.schema.parser.FieldElement
import com.microsoft.thrifty.schema.parser.ScalarTypeElement
import com.microsoft.thrifty.schema.parser.TypeElement
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FieldTest {
    private var location: Location = Location.get("", "")
    private var fieldId: Int = 1
    private var fieldName: String = "foo"
    private var fieldType: TypeElement = ScalarTypeElement(location, "i32", null)
    private var requiredness: Requiredness = Requiredness.DEFAULT
    private var annotations: AnnotationElement? = null
    private var documentation: String = ""

    @Test
    fun requiredFields() {
        requiredness = Requiredness.REQUIRED
        val element = field()

        val field = Field(element, emptyMap())
        field.required shouldBe true
        field.optional shouldBe false
    }

    @Test
    fun optionalFields() {
        requiredness = Requiredness.OPTIONAL
        val element = field()
        val field = Field(element, emptyMap())
        field.required shouldBe false
        field.optional shouldBe true
    }

    @Test
    fun defaultFields() {
        val element = field()
        val field = Field(element, emptyMap())
        field.required shouldBe false
        field.optional shouldBe false
    }

    @Test
    fun unredactedAndUnobfuscatedByDefault() {
        val element = field()
        val field = Field(element, emptyMap())
        field.isRedacted shouldBe false
        field.isObfuscated shouldBe false
    }

    @Test
    fun redactedByThriftAnnotation() {
        annotations = annotation("thrifty.redacted")
        val element = field()

        val field = Field(element, emptyMap())
        field.isRedacted shouldBe true
    }

    @Test
    fun redactedByShortThriftAnnotation() {
        annotations = annotation("redacted")
        val element = field()

        val field = Field(element, emptyMap())
        field.isRedacted shouldBe true
    }

    @Test
    fun redactedByJavadocAnnotation() {
        documentation = "/** @redacted */"
        val element = field()

        val field = Field(element, emptyMap())
        field.isRedacted shouldBe true
    }

    @Test
    fun obfuscatedByThriftAnnotation() {
        annotations = annotation("thrifty.obfuscated")
        val element = field()

        val field = Field(element, emptyMap())
        field.isObfuscated shouldBe true
    }

    @Test
    fun obfuscatedByShortThriftAnnotation() {
        annotations = annotation("obfuscated")
        val element = field()

        val field = Field(element, emptyMap())
        field.isObfuscated shouldBe true
    }

    @Test
    fun obfuscatedByJavadocAnnotation() {
        documentation = "/** @obfuscated */"
        val element = field()

        val field = Field(element, emptyMap())
        field.isObfuscated shouldBe true
    }

    @Test
    fun builderCreatesCorrectField() {
        val fieldElement = field()
        val field = Field(fieldElement, emptyMap())

        val annotations = emptyMap<String, String>()
        val thriftType = BuiltinType.DOUBLE

        val builderField = field.toBuilder()
                .annotations(annotations)
                .type(thriftType)
                .build()

        builderField.annotations shouldBe annotations
        builderField.type shouldBe thriftType
    }

    private fun annotation(name: String): AnnotationElement {
        return AnnotationElement(Location.get("", ""), mapOf(name to "true"))
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