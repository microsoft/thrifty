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
import com.microsoft.thrifty.schema.parser.EnumElement
import com.microsoft.thrifty.schema.parser.EnumMemberElement
import com.microsoft.thrifty.schema.parser.FieldElement
import com.microsoft.thrifty.schema.parser.FunctionElement
import com.microsoft.thrifty.schema.parser.ServiceElement
import com.microsoft.thrifty.schema.parser.StructElement
import com.microsoft.thrifty.schema.parser.TypedefElement

import java.util.Locale
import java.util.UUID

/**
 * A mixin encapsulating a common implementation of [UserElement],
 * which does not conveniently fit in a single base class.
 */
internal class UserElementMixin : UserElement {
    private val _uuid: UUID
    private val _name: String
    private val _location: Location
    private val _documentation: String
    private val _annotations: ImmutableMap<String, String>

    override val isDeprecated: Boolean
        get() = hasThriftOrJavadocAnnotation("deprecated")

    constructor(struct: StructElement) : this(struct.uuid, struct.name, struct.location, struct.documentation, struct.annotations)
    constructor(field: FieldElement) : this(field.uuid, field.name, field.location, field.documentation, field.annotations)
    constructor(enumElement: EnumElement) : this(enumElement.uuid, enumElement.name, enumElement.location, enumElement.documentation, enumElement.annotations)
    constructor(member: EnumMemberElement) : this(member.uuid, member.name, member.location, member.documentation, member.annotations)
    constructor(element: TypedefElement) : this(element.uuid, element.newName, element.location, element.documentation, element.annotations)
    constructor(element: ServiceElement) : this(element.uuid, element.name, element.location, element.documentation, element.annotations)
    constructor(element: FunctionElement) : this(element.uuid, element.name, element.location, element.documentation, element.annotations)

    constructor(
            uuid: UUID,
            name: String,
            location: Location,
            documentation: String,
            annotationElement: AnnotationElement?) {
        this._uuid = uuid
        this._name = name
        this._location = location
        this._documentation = documentation

        val annotations = ImmutableMap.builder<String, String>()
        if (annotationElement != null) {
            annotations.putAll(annotationElement.values)
        }
        this._annotations = annotations.build()
    }

    private constructor(builder: Builder) {
        this._uuid = builder.uuid
        this._name = builder.name
        this._location = builder.location
        this._documentation = builder.documentation
        this._annotations = builder.annotations
    }

    override fun uuid(): UUID {
        return _uuid
    }

    override fun location(): Location {
        return _location
    }

    override fun name(): String {
        return _name
    }

    override fun documentation(): String {
        return _documentation
    }

    override fun annotations(): ImmutableMap<String, String> {
        return _annotations
    }

    /**
     * Checks for the presence of the given annotation name, in several possible
     * varieties.  Returns true if:
     *
     *
     *  * A Thrift annotation matching the exact name is present
     *  * A Thrift annotation equal to the string "thrifty." plus the name is present
     *  * The Javadoc contains "@" plus the annotation name
     *
     *
     * The latter two conditions are officially undocumented, but are present for
     * legacy use.  This behavior is subject to change without notice!
     */
    fun hasThriftOrJavadocAnnotation(name: String): Boolean {
        return (annotations().containsKey(name)
                || annotations().containsKey("thrifty.$name")
                || hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@$name"))
    }

    override fun toString(): String {
        return ("UserElementMixin{"
                + "uuid='" + _uuid + '\''.toString()
                + ", name='" + _name + '\''.toString()
                + ", location=" + _location
                + ", documentation='" + _documentation + '\''.toString()
                + ", annotations=" + _annotations
                + '}'.toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as UserElementMixin

        if (_uuid != other._uuid) return false
        if (_name != other._name) return false
        if (_location != other._location) return false
        return if (_documentation != other._documentation) false else _annotations == other._annotations

    }

    override fun hashCode(): Int {
        var result = _uuid.hashCode()
        result = 31 * result + _name.hashCode()
        result = 31 * result + _location.hashCode()
        result = 31 * result + _documentation.hashCode()
        result = 31 * result + _annotations.hashCode()
        return result
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    internal class Builder internal constructor(userElement: UserElement) {
        var uuid: UUID = userElement.uuid()
        var name: String = userElement.name()
        var location: Location = userElement.location()
        var documentation: String = userElement.documentation()
        var annotations: ImmutableMap<String, String> = userElement.annotations()

        fun uuid(uuid: UUID): Builder = apply {
            this.uuid = uuid
        }

        fun name(name: String): Builder = apply {
            this.name = name
        }

        fun location(location: Location): Builder = apply {
            this.location = location
        }

        fun documentation(documentation: String): Builder = apply {
            this.documentation = if (isNonEmptyJavadoc(documentation)) {
                documentation
            } else {
                ""
            }
        }

        fun annotations(annotations: Map<String, String>): Builder = apply {
            this.annotations = ImmutableMap.copyOf(annotations)
        }

        fun build(): UserElementMixin = UserElementMixin(this)
    }
}
