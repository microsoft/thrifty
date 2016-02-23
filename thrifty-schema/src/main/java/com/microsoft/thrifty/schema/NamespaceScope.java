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
package com.microsoft.thrifty.schema;

public enum NamespaceScope {
    ALL("*"),
    CPP("cpp"),
    JAVA("java"),
    PY("py"),
    PY_TWISTED("py.twisted"),
    PERL("perl"),
    RB("rb"),
    COCOA("cocoa"),
    CSHARP("csharp"),
    PHP("php"),
    SMALLTALK_CATEGORY("smalltalk.category"),
    SMALLTALK_PREFIX("smalltalk.prefix"),
    C_GLIB("cglib"),
    GO("go"),
    LUA("lua"),
    ST("st"),
    DELPHI("delphi"),
    JAVASCRIPT("js"),
    UNKNOWN("none");

    private final String name;

    NamespaceScope(String name) {
        this.name = name;
    }

    public static NamespaceScope forThriftName(String name) {
        for (NamespaceScope scope : values()) {
            if (scope.name.equals(name)) {
                return scope;
            }
        }
        return null;
    }
}
