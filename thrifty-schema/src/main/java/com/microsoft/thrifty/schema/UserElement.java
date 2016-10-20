package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableMap;

/**
 * Represents data common to user-defined elements of a Thrift program.
 */
public interface UserElement {
    /**
     * Gets the name of the element.
     */
    String name();

    /**
     * Gets the {@link Location} where the element is defined.
     */
    Location location();

    /**
     * Gets the documentation comments of the element, or an empty string.
     */
    String documentation();

    /**
     * Gets an immutable map containing any annotations present on the element.
     */
    ImmutableMap<String, String> annotations();

    /**
     * Gets a value indicating whether the element contains non-empty Javadoc.
     */
    boolean hasJavadoc();

    /**
     * Gets a value indicating whether the element has been marked as
     * deprecated; this may or may not be meaningful, depending on the
     * particular type of element.
     */
    boolean isDeprecated();
}
