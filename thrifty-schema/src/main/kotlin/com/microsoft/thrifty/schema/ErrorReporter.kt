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

class ErrorReporter {
    var hasError = false
        private set

    private val reports_: MutableList<Report> = mutableListOf()

    val reports: List<Report>
        get() = reports_

    fun warn(location: Location, message: String) {
        reports_.add(Report(Level.WARNING, location, message))
    }

    fun error(location: Location, message: String) {
        hasError = true
        reports_.add(Report(Level.ERROR, location, message))
    }

    fun formattedReports(): List<String> {
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        for (report in reports_) {
            when (report.level) {
                ErrorReporter.Level.WARNING -> sb.append("W: ")
                ErrorReporter.Level.ERROR -> sb.append("E: ")
            }

            sb.append(report.message)
            sb.append(" (at ")
            sb.append(report.location)
            sb.append(")")

            list += sb.toString()

            sb.setLength(0)
        }
        return list
    }

    data class Report(
            val level: Level,
            val location: Location,
            val message: String
    )

    enum class Level {
        WARNING,
        ERROR
    }
}