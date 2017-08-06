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

import com.google.common.collect.ImmutableMap;

import java.util.UUID;

/**
 * Represents data common to user-defined elements of a Thrift program.
 */
public interface UserElement {
    /**
     * A globally unique ID for this element. This is useful for cases where you newBuilder() an element to change it
     * (such as in a Schema preprocessor) and want to update references from other objects in a deterministic way that
     * matches IDs. If you want a new instance of an object that is unrelated, you should change this value.
     *
     * @return the uuid of this element.
     */
    UUID uuid();

    /**
     * Gets the name of the element.
     *
     * @return the name of this element.
     */
    String name();

    /**
     * Gets the {@link Location} where the element is defined.
     *
     * @return the Location where this element is defined.
     */
    Location location();

    /**
     * Gets the documentation comments of the element, or an empty string.
     *
     * @return the documentation present on this element, or an empty string.
     */
    String documentation();

    /**
     * Gets a value indicating whether the element contains non-empty Javadoc.
     *
     * @return true if this element contains non-empty Javadoc.
     */
    default boolean hasJavadoc() {
        return isNonEmptyJavadoc(documentation());
    }

    /**
     * Gets an immutable map containing any annotations present on the element.
     *
     * @return all annotations present on this element.
     */
    ImmutableMap<String, String> annotations();

    /**
     * Gets a value indicating whether the element has been marked as
     * deprecated; this may or may not be meaningful, depending on the
     * particular type of element.
     *
     * @return true if this element has been marked as deprecated.
     */
    boolean isDeprecated();

    /**
     * Returns {@code true} if {@code doc} is non-empty Javadoc, otherwise
     * {@code false}.
     */
    static boolean isNonEmptyJavadoc(String doc) {
        if (doc == null) return false;
        if (doc.isEmpty()) return false;

        for (int i = 0; i < doc.length(); ++i) {
            char c = doc.charAt(i);
            if (!Character.isWhitespace(c)) {
                return true;
            }
        }

        return false;
    }
}
