/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.schema.render

import com.bendb.thrifty.schema.*
import java.io.File

internal val NEWLINE = System.getProperty("line.separator")!!
internal val DOUBLE_NEWLINE = "$NEWLINE$NEWLINE"

/**
 * Full file path for a given [UserElement].
 */
internal inline val UserElement.filepath: String
    get() = "${location.base}${File.separator}${location.path}"

/**
 * Full file path for a given [Location].
 */
internal inline val Location.filepath: String
    get() = "$base${File.separator}$path"

/**
 * A string representation for a [Field's][Field] requiredness for rendering.
 */
internal val Field.requiredness: String
    get() {
        return when {
            required -> " required"
            optional -> " optional"
            else -> ""
        }
    }

/**
 * Unpacks a [ThriftType] as needed, handling collection types.
 *
 * @return the unpacked [ThriftType].
 */
internal fun ThriftType.unpack(): Set<ThriftType> {
    return when {
        this is ListType -> setOf(elementType)
        this is SetType -> setOf(elementType)
        this is MapType -> setOf(keyType, valueType)
        else -> setOf(this)
    }
}

/**
 * Invokes the action from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * This is slightly modified form of [Iterable.joinTo] but does not transform each element to a string. Instead the buffer
 * is passed to the [action] so that it can run its own additions to the [buffer].
 */
internal fun <T, A : Appendable> Iterable<T>.joinEachTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    action: ((A, T) -> Unit)? = null
): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            action?.invoke(buffer, element)
        } else break
    }
    if (limit in 0..(count - 1)) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}
