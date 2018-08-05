/*
 * Copyright (C) 2015-2018 Microsoft Corporation
 * Copyright (C) 2015 Square, Inc.
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
package com.microsoft.thrifty.schema

import java.io.File
import java.nio.file.Paths

/**
 * Represents a location within a text file on a filesystem.
 *
 * @property base A location on the filesystem, to with [path] is relative.
 * @property path The path, relative to [base], of the file located by this object.
 * @property line The line number identified by this object, starting with 1.
 * @property column The column identified by this object, starting with 1.
 */
class Location private constructor(
        val base: String,
        val path: String,
        val line: Int,
        val column: Int
) {
    init {
        require(line > 0 || line == -1) { "line: $line" }
        require(column > 0 || column == -1) { "column: $column"}
    }

    /**
     * Computes and returns the Thrift 'program' name, which is the filename portion
     * of the full path *without* the .thrift extension.
     *
     * @return the Thrift program name representing this file.
     */
    val programName: String
        get() {
            var name = Paths.get(path).fileName.toString()
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex != -1) {
                name = name.substring(0, dotIndex)
            }
            return name
        }

    /**
     * Returns a copy of this object pointing to the given line and column.
     */
    fun at(line: Int, column: Int): Location {
        return Location(base, path, line, column)
    }

    override fun toString(): String {
        val sb = StringBuilder(base.length + path.length)
        if (!base.isEmpty()) {
            sb.append(base).append(File.separator)
        }
        sb.append(path)
        if (line != -1) {
            sb.append(" at ").append(line)
            if (column != -1) {
                sb.append(":").append(column)
            }
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Location) {
            val location = other as Location?

            if (line != location!!.line) return false
            if (column != location.column) return false
            return if (base != location.base) false else path == location.path
        }

        return false
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + line
        result = 31 * result + column
        return result
    }

    companion object {
        fun get(base: String, path: String): Location {
            return Location(base, path, -1, -1)
        }
    }
}
