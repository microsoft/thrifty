package com.bendb.thrifty.schema;

/**
 * A description of whether and when a field may be absent from a struct,
 * union, or exception.
 *
 * <p>The semantics of required, optional, and the default are underspecified
 * at present.  Intuitively, `required` should mean that the struct containing
 * the field cannot be valid unless the field is present, where `optional`
 * would indicate that the field's presence does not affect the struct's
 * validity.  The actual impact of these modifiers are subtler than this, and
 * are defined by the implementation and code generator(s) used.
 *
 * <p>For a more thorough treatment, see
 * <a href="http://lionet.livejournal.com/66899.html">this blog post</a> which,
 * though being five years old at the time of writing, is still relevant.
 */
public enum Requiredness {
    /**
     * No requirement.  The semantics of this are undefined, but in practice
     * can mean required for serialization, optional for deserialization.
     *
     * <strong>This is not consistent between server implementations.</strong>
     *
     * For Thrifty's purposes, this is treated as optional - which appears to
     * align with the Python server implementation
     */
    DEFAULT,

    /**
     * Required.  In practice this means that a field must be present both on
     * serialization and on deserialization.
     */
    REQUIRED,

    /**
     * Optional fields may be unset at all times.
     */
    OPTIONAL
}
