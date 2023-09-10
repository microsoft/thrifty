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
package com.bendb.thrifty.schema

/**
 * An enumeration of all namespace-having programming languages recognized by
 * Thrifty.
 *
 * Most scopes in this enumeration are irrelevant to Thrifty, but are specified
 * by Thrift and so are included to retain fidelity.  Scopes relevant to
 * Thrifty are [ALL], [JAVA], and [KOTLIN].
 */
enum class NamespaceScope(internal val thriftName: String) {
    /**
     * Represents all languages; often used as a fallback value.
     */
    ALL("*"),

    /**
     * Represents a C++ namespace scope.
     */
    CPP("cpp"),

    /**
     * Represents a Java namespace scope.
     *
     * Usually redundant with [KOTLIN] or [JVM], but is available for use in those cases
     * where generated Java code should not share a namespace with generated
     * Kotlin code.
     */
    JAVA("java"),

    /**
     * Represents a Python namespace scope.
     */
    PY("py"),

    /**
     * Represents a Python namespace scope, specifically for Twisted.
     */
    PY_TWISTED("py.twisted"),

    /**
     * Represents a Perl namespace scope.
     */
    PERL("perl"),

    /**
     * Represents a Ruby namespace scope.
     */
    RB("rb"),

    /**
     * Represents an Objective-C namespace scope.
     */
    COCOA("cocoa"),

    /**
     * Represents a C# namespace scope.
     */
    CSHARP("csharp"),

    /**
     * Represents a PHP namespace scope.
     */
    PHP("php"),

    /**
     * Represents a Smalltalk Category.
     */
    SMALLTALK_CATEGORY("smalltalk.category"),

    /**
     * Represents a Smalltalk prefix.
     */
    SMALLTALK_PREFIX("smalltalk.prefix"),

    /**
     * Represents a CGLIB scope.  I'm not sure what this is.
     */
    C_GLIB("cglib"),

    /**
     * Represents a Go namespace scope.
     */
    GO("go"),

    /**
     * Represents a Lua namespace scope.
     */
    LUA("lua"),

    /**
     * Represents a Smalltalk namespace scope.
     */
    ST("st"),

    /**
     * Represents a Delphi namespace scope.
     */
    DELPHI("delphi"),

    /**
     * Represents a Javascript namespace scope.
     */
    JAVASCRIPT("js"),

    /**
     * Represents a namespace scope for an unknown or unrecognized language.
     */
    UNKNOWN("none"),

    /**
     * Represents a Kotlin namespace scope.
     *
     * Usually redundant with [JAVA] or [JVM], but is available for use in those cases
     * where generated Kotlin code should not share a namespace with generated
     * Java code.
     */
    KOTLIN("kt"),

    /**
     * Represents a JVM namespace scope.
     */
    JVM("jvm");

    companion object {
        /**
         * Returns the [NamespaceScope] identified by the given [name], or null
         * if no such scope exists.
         */
        fun forThriftName(name: String): NamespaceScope? {
            for (scope in values()) {
                if (scope.thriftName == name) {
                    return scope
                }
            }
            return null
        }

        /**
         * @return a [Set] of all the members that are [JVM] sub-types.
         */
        fun jvmMembers() = setOf(JAVA, KOTLIN)
    }
}
