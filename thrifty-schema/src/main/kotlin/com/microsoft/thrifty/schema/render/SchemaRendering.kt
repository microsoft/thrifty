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
@file:JvmName("SchemaRendering")

package com.microsoft.thrifty.schema.render

import com.microsoft.thrifty.schema.*
import com.microsoft.thrifty.schema.NamespaceScope.JAVA
import com.microsoft.thrifty.schema.parser.ConstValueElement
import java.io.File

/*
 * Rendering utilities for Thrifty elements. These render Thrifty elements back to well-formatted
 * spec notation.
 */

/**
 * Renders a potentially multi-file schema as a [Set] of [ThriftSpec]s. This will resolve `include`s
 * for the files while also collecting namespaces.
 *
 * @param relativizeIncludes a flag to indicate whether or not to relativize include statements.
 * Default is `true`
 * @param namespaceResolver a lambda function to result namespaces for given [UserType]s. Default
 * is to just use its Java namespace, but can be useful to configure it to look for alternate
 * namespaces (such as when performing package name preprocessing). This parameter will likely be
 * removed in the future.
 * @param minimumPrefix an optional "minimum prefix" to require if [relativizeIncludes] is true.
 * Normally when relativizing, paths are shortened to remove their combined common prefix. This can
 * be specified to ensure that a minimumPrefix is kept for reference beyond the scope of this
 * function. Example: `minPrefix = "foo/bar"` -> common prefix with `bar/baz` will be `foo/bar/baz`.
 * @return the rendered [Set] of [ThriftSpec]s.
 */
fun Schema.multiFileRender(
    relativizeIncludes: Boolean = true,
    namespaceResolver: (UserType) -> String = { it.namespaces[JAVA]!! },
    minimumPrefix: String? = null
): Set<ThriftSpec> {
    // If relativizing, deduce the common prefix of all the file paths to know the "root" of their
    // directory
    val commonPathPrefix = if (relativizeIncludes) {
        elements()
            .asSequence()
            .map(UserElement::filepath)
            .reduce { currentPrefix, nextLocation ->
                currentPrefix.commonPrefixWith(nextLocation)
            }
            .let { calculatedPrefix ->
                minimumPrefix?.let { minPrefix ->
                    check(calculatedPrefix.contains(minPrefix)) {
                        "Calculated common prefix for files doesn't contain the specified minimum prefix!\nCalculated: $calculatedPrefix\nMinimum: $minPrefix"
                    }
                    calculatedPrefix.substringBefore(minPrefix)
                } ?: calculatedPrefix
            }
            .let {
                if (it.endsWith(".thrift")) {
                    // We only have one file. Back it up to the directory name for sanity
                    it.substringBeforeLast(File.separator)
                } else it
            }
    } else ""
    return elements()
        .groupBy(UserElement::filepath)
        .mapKeys { it.key.removePrefix(commonPathPrefix) }
        .mapTo(LinkedHashSet()) { (filePath, sourceElements) ->
            val elements =
                sourceElements.filter { it.filepath.removePrefix(commonPathPrefix) == filePath }
            val namespaces = elements.filterIsInstance<UserType>()
                .map(UserType::namespaces)
            check(namespaces.distinct().size == 1) {
                "Multiple namespaces! $namespaces"
            }
            val realNamespaces = namespaces.first()
            val fileSchema = toBuilder()
                .exceptions(elements.filterIsInstance<StructType>().filter(StructType::isException))
                .services(elements.filterIsInstance<ServiceType>())
                .structs(elements.filterIsInstance<StructType>().filter { !it.isUnion && !it.isException })
                .typedefs(elements.filterIsInstance<TypedefType>())
                .enums(elements.filterIsInstance<EnumType>())
                .unions(elements.filterIsInstance<StructType>().filter(StructType::isUnion))
                .build()

            val sourceFile = File(filePath)
            val includes = elements
                .flatMap { element ->
                    when (element) {
                        is StructType -> {
                            element.fields
                                .flatMap {
                                    it.type
                                        .unpack()
                                }
                        }
                        is ServiceType -> {
                            element.methods
                                .flatMap { method ->
                                    (method.run { exceptions + parameters })
                                        .flatMap {
                                            it.type
                                                .unpack()
                                        } + method.returnType.unpack()
                                }
                        }
                        is TypedefType -> element.oldType.unpack()
                        else -> emptySet()
                    }
                }
                .filterIsInstance<UserType>()
                .distinctBy(UserType::filepath)
                .filter { it.filepath.removePrefix(commonPathPrefix) != filePath }
                .map { it to it.filepath.removePrefix(commonPathPrefix) }
                .run {
                    if (relativizeIncludes) {
                        map {
                            it.first to File(it.second).toRelativeString(sourceFile)
                                .removePrefix("../")
                                .run {
                                    if (startsWith("../")) {
                                        this
                                    } else {
                                        "./$this"
                                    }
                                }
                        }
                    } else this
                }
                .map {
                    Include(
                        path = it.second,
                        namespace = namespaceResolver(it.first),
                        relative = relativizeIncludes
                    )
                }

            return@mapTo ThriftSpec(
                filePath = filePath,
                namespaces = realNamespaces,
                includes = includes,
                schema = fileSchema
            )
        }
}

/**
 * @return the rendered form of this [Schema].
 */
fun Schema.render() = renderTo(StringBuilder()).toString()

/**
 * Renders this [Schema] into a given [buffer].
 *
 * @return the [buffer] for chaining convenience.
 */
@Suppress("RemoveExplicitTypeArguments") // False positive
fun <A : Appendable> Schema.renderTo(buffer: A) = buffer.apply {
    if (typedefs.isNotEmpty()) {
        typedefs
            .sortedWith(Comparator { o1, o2 ->
              // Sort by the type first, then the name. This way we can group types together
              val typeComparison = o1.oldType.name.compareTo(o2.oldType.name)
              return@Comparator if (typeComparison != 0) {
                typeComparison
              } else {
                o1.name.compareTo(o2.name)
              }
            })
            .joinEachTo(
                buffer = buffer,
                separator = DOUBLE_NEWLINE,
                postfix = DOUBLE_NEWLINE
            ) { _, typedef ->
                typedef.renderTo<A>(buffer)
            }
    }
    if (enums.isNotEmpty()) {
        enums.sortedBy(EnumType::name)
            .joinEachTo(
                buffer = buffer,
                separator = DOUBLE_NEWLINE,
                postfix = DOUBLE_NEWLINE
            ) { _, enum ->
                enum.renderTo<A>(buffer)
            }
    }
    if (structs.isNotEmpty()) {
        structs.sortedBy(StructType::name)
            .joinEachTo(
                buffer = buffer,
                separator = DOUBLE_NEWLINE,
                postfix = DOUBLE_NEWLINE
            ) { _, struct ->
                struct.renderTo<A>(buffer)
            }
    }
    if (unions.isNotEmpty()) {
        unions.sortedBy(StructType::name)
            .joinEachTo(
                buffer = buffer,
                separator = DOUBLE_NEWLINE,
                postfix = DOUBLE_NEWLINE
            ) { _, struct ->
                struct.renderTo<A>(buffer)
            }
    }
    if (exceptions.isNotEmpty()) {
        exceptions.sortedBy(StructType::name)
            .joinEachTo(
                buffer = buffer,
                separator = DOUBLE_NEWLINE,
                postfix = DOUBLE_NEWLINE
            ) { _, struct ->
                struct.renderTo<A>(buffer)
            }
    }
    if (services.isNotEmpty()) {
        services.sortedBy(ServiceType::name)
            .joinEachTo(
                buffer = buffer,
                separator = DOUBLE_NEWLINE,
                postfix = DOUBLE_NEWLINE
            ) { _, service ->
                service.renderTo<A>(buffer)
            }
    }

}

/**
 * @return the rendered form of this [UserElement].
 */
fun UserElement.renderElement(indent: String = "  ") =
    renderElementTo(StringBuilder(), indent).toString()

/**
 * Renders this [UserElement] into a given [buffer].
 *
 * @return the [buffer] for chaining convenience.
 */
fun <A : Appendable> UserElement.renderElementTo(buffer: A, indent: String = "  "): A {
    @Suppress("RemoveExplicitTypeArguments") // False positive
    when (this) {
        is UserType -> renderTo<A>(buffer)
        is Field -> renderTo<A>(buffer, indent)
        is ServiceMethod -> renderTo<A>(buffer, indent)
        is EnumMember -> renderTo<A>(buffer, indent)
        is Constant -> renderTo<A>(buffer)
        else -> throw IllegalArgumentException("Unsupported UserElement type: $this")
    }
    return buffer
}

/**
 * @return the rendered form of this [UserType].
 */
fun UserType.render(): String = renderTo(StringBuilder()).toString()

/**
 * Renders this [UserType] into a given [buffer].
 *
 * @return the [buffer] for chaining convenience.
 */
fun <A : Appendable> UserType.renderTo(buffer: A): A {
    // Doesn't follow the usual buffer.apply function body pattern because type checking falls over
    @Suppress("RemoveExplicitTypeArguments") // False positive
    when (this) {
        is TypedefType -> renderTo<A>(buffer)
        is EnumType -> renderTo<A>(buffer)
        is StructType -> renderTo<A>(buffer)
        is ServiceType -> renderTo<A>(buffer)
        else -> throw IllegalArgumentException("Unrecognized UserType: $this")
    }
    return buffer
}

private fun <A : Appendable> TypedefType.renderTo(buffer: A) = buffer.apply {
    renderJavadocTo(buffer)
    append("typedef ")
    oldType.renderTypeTo(buffer, location)
    oldType.annotations
        .renderTo(buffer)
    append(" ", name)
    renderAnnotationsTo(buffer, indent = " ")
}

private fun <A : Appendable> StructType.renderTo(buffer: A) = buffer.apply {
    renderJavadocTo(buffer)
    val type = when {
        isException -> "exception"
        isUnion -> "union"
        else -> "struct"
    }
    append(type, " ", name, " {")
    appendln()
    fields
        .joinEachTo(buffer, NEWLINE) { _, field ->
            field.renderElementTo(buffer)
        }
    appendln()
    append("}")
    renderAnnotationsTo(buffer)
}

private fun <A : Appendable> EnumType.renderTo(buffer: A) = buffer.apply {
    renderJavadocTo(buffer)
    append("enum ", name, " {")
    appendln()
    members.joinEachTo(buffer, ",$NEWLINE") { _, member ->
        member.renderElementTo(buffer)
    }
    appendln()
    append("")
    append("}")
    renderAnnotationsTo(buffer)
}

private fun <A : Appendable> ServiceType.renderTo(buffer: A) = buffer.apply {
    renderJavadocTo(buffer)
    append("service ", name, " {")
    appendln()
    methods.joinEachTo(buffer = buffer, separator = DOUBLE_NEWLINE) { _, method ->
        method.renderElementTo(buffer)
    }
    appendln()
    append("}")
    renderAnnotationsTo(buffer)
}

private fun <A : Appendable> Field.renderTo(buffer: A, indent: String = "  ") = buffer.apply {
    renderJavadocTo(buffer, indent)
    append(indent, id.toString(), ":", requiredness, " ")
    type.renderTypeTo(buffer, location)
    if (type !is UserType) type.annotations.renderTo(buffer)
    append(" ", name)
    defaultValue?.renderTo(buffer)
    renderAnnotationsTo(buffer, indent)
}

private fun <A : Appendable> ServiceMethod.renderTo(buffer: A, indent: String = "  ") =
    buffer.apply {
        renderJavadocTo(buffer, indent)
        append(indent)
        returnType.renderTypeTo(buffer, location)
        append(" ", name)
        if (parameters.isEmpty()) {
            append("()")
        } else {
            parameters
                .joinEachTo(
                    buffer = buffer,
                    separator = ",$NEWLINE",
                    prefix = "($NEWLINE",
                    postfix = "$NEWLINE$indent)"
                ) { _, param ->
                    param.renderTo(buffer, "$indent  ")
                }
        }
        if (exceptions.isNotEmpty()) {
            appendln(" throws (")
            exceptions
                .joinEachTo(buffer = buffer, separator = ",$NEWLINE") { _, param ->
                    param.renderTo(buffer, "$indent  ")
                }
            appendln()
            append(indent, ")")
        }
        renderAnnotationsTo(buffer, indent)
    }

private fun <A : Appendable> EnumMember.renderTo(buffer: A, indent: String = "  ") = buffer.apply {
    renderJavadocTo(buffer, indent)
    append(indent, name, " = ", value.toString())
    renderAnnotationsTo(buffer)
}

private fun <A : Appendable> Constant.renderTo(buffer: A) = buffer.apply {
    renderJavadocTo(buffer)
    append("const ")
    type.renderTypeTo(buffer, location)
    append(" ")
    type.annotations
        .renderTo(buffer)
    append(" ", name)
    value.renderTo(buffer)
    renderAnnotationsTo(buffer)
}

private fun <A : Appendable> ConstValueElement.renderTo(buffer: A, prefix: String = " ") = buffer.apply {
    append(prefix, "= ", thriftText)
}

/**
 * Renders a thrift type by its name, possibly prefixing with the program name if not the same
 * location as [source].
 */
private fun <A : Appendable> ThriftType.renderTypeTo(buffer: A, source: Location): A {
    // Doesn't follow the usual buffer.apply function body pattern because type checking falls over
    when {
        this is UserType && source.filepath != location.filepath -> {
            buffer.apply {
                append(location.programName)
                append(".")
                append(name)
            }
        }
        this is SetType -> {
            buffer.apply {
                append("set<")
                elementType.renderTypeTo(buffer, source)
                append(">")
            }
        }
        this is ListType -> {
            buffer.apply {
                append("list<")
                elementType.renderTypeTo(buffer, source)
                append(">")
            }
        }
        this is MapType -> {
            buffer.apply {
                append("map<")
                keyType.renderTypeTo(buffer, source)
                append(",")
                valueType.renderTypeTo(buffer, source)
                append(">")
            }
        }
        else -> buffer.append(name)
    }
    return buffer
}

private fun <A : Appendable> UserElement.renderJavadocTo(buffer: A, indent: String = "") =
    buffer.apply {
        if (hasJavadoc) {
            val docLines = documentation.trim()
                .trim(Character::isSpaceChar)
                .lines()
            val isSingleLine = docLines.size == 1
            if (isSingleLine) {
                append(indent)
                append("/* ")
                append(docLines[0])
                appendln(" */")
            } else {
                docLines.joinTo(
                    buffer = buffer,
                    separator = NEWLINE,
                    prefix = "$indent/**$NEWLINE",
                    postfix = "$NEWLINE$indent */$NEWLINE"
                ) {
                    "$indent * $it"
                }
            }
        }
    }

private fun <A : Appendable> UserElement.renderAnnotationsTo(
    buffer: A,
    indent: String = "",
    prefix: String = " "
) = buffer.apply {
    annotations.renderTo(buffer, indent, prefix)
}

private fun <A : Appendable> Map<String, String>.renderTo(
    buffer: A,
    indent: String = "",
    prefix: String = " "
) = buffer.apply {
    when {
        size == 1 -> {
            append(prefix)
            append("(")
            val (key, value) = entries.first()
            append(key)
            append(" = ")
            append("\"")
            append(value.replace("\"", "\\\""))
            append("\"")
            append(")")
        }
        size > 1 -> {
            append(prefix)
            appendln("(")
            entries
                .sortedBy(Map.Entry<String, String>::key)
                .joinTo(buffer = buffer, separator = ",$NEWLINE") { (key, value) ->
                    "$indent  $key = \"${value.replace("\"", "\\\"")}\""
                }
            appendln()
            append(indent, ")")
        }
    }
}
