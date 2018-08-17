package com.microsoft.thrifty.schema.render

/**
 * Represents an include in a [ThriftSpec].
 *
 * @property path the path to the included file. Can be relative or absolute.
 * @property namespace the namespace of this included file (used to differentiate this from the name space of the source file).
 * @property relative a flag indicating whether or not this [path] is a relative path.
 */
data class Include internal constructor(val path: String, internal val namespace: String, val relative: Boolean)
