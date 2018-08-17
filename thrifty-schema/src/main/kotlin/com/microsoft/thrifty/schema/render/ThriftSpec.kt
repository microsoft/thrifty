package com.microsoft.thrifty.schema.render

import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.Schema
import java.io.File

/**
 * A representation of an individual thrift spec file.
 *
 * @property filePath the relative path of this file.
 * @property namespaces the namespaces of this file.
 * @property includes any includes this file declares.
 * @property schema the [Schema] of elements contained within this file.
 */
data class ThriftSpec internal constructor(
    val filePath: String,
    val namespaces: Map<NamespaceScope, String>,
    val includes: List<Include>,
    val schema: Schema
) {

    /**
     * The simple name of this file.
     */
    val name = File(filePath).nameWithoutExtension

    /**
     * @return a render of this file. This returns valid thrift that could be parsed back and yield
     * an equivalent [Schema] to this spec's [schema].
     */
    fun render(fileComment: String? = null) = renderTo(StringBuilder(), fileComment).toString()

    /**
     * Renders this file to a given [buffer]. This returns valid thrift that could be parsed back and
     * yield an equivalent [Schema] to this spec's [schema].
     *
     * @return the [buffer] for convenience chaining.
     */
    fun <A : Appendable> renderTo(buffer: A, fileComment: String? = null) = buffer.apply {
        fileComment?.let {
            append("// ", it)
            appendln()
            appendln()
        }
        if (namespaces.isNotEmpty()) {
            namespaces.entries.joinEachTo(
                buffer = buffer,
                separator = NEWLINE,
                postfix = DOUBLE_NEWLINE
            ) { _, (key, value) ->
                buffer.append("namespace ", key.thriftName, " ", value)
            }
        }
        if (includes.isNotEmpty()) {
            includes
                .sortedBy(Include::path)
                .joinEachTo(buffer,
                    NEWLINE, postfix = DOUBLE_NEWLINE
                ) { _, include ->
                    buffer.append("include \"", include.path, "\"")
                }
        }
        schema.renderTo(this)
    }
}
