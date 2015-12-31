package com.bendb.thrifty.schema;

/**
 * A description of whether and when a field may be absent from a struct,
 * union, or exception.
 *
 * <p>The semantics of required, optional, and the default are underspecified
 * at present.  Intuitively, `required` should mean that the struct containing
 * the field cannot be valid unless the field is present, where `optional`
 * would indicate that the field's presence does not affect the struct's
 * validity.  The actual impacts of these modifiers are both more subtle and
 * implementation-defined, varying both by runtime library and code-generator
 * implementations.
 *
 * <p>For a more thorough treatment of the subject, see
 * <a href="http://lionet.livejournal.com/66899.html">this blog post</a> which,
 * though being five years old at the time of writing, is still relevant.  Read
 * on for my own interpretation of things.
 *
 * <h2>What does the official implementation do?</h2>
 *
 * <p>There are two times when requiredness modifiers affect behavior - during
 * serialization and during struct validation.  During serialization, they
 * affect whether a field will be written to the underlying protocol.  During
 * validation, they determine whether an unset field will cause validation to
 * fail.  As this is not documented anywhere, a close reading of the Apache
 * implementation's Java code generator yields the following tables, describing
 * the actual impact of the modifiers:
 *
 * <h3>Will an unset field be serialized?</h3>
 *
 * The question refers to whether writing a struct to a protocol will result in
 * a beginField, field, endField sequence being emitted.
 *
 * <table summary="Will a field be serialized?" border="1">
 *     <thead>
 *         <tr>
 *             <th></th>
 *             <th>Value types</th>
 *             <th>Objects</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td><strong>Default</strong></td>
 *             <td>always*</td>
 *             <td>if set</td>
 *         </tr>
 *         <tr>
 *             <td><strong>Optional</strong></td>
 *             <td>if set</td>
 *             <td>if set</td>
 *         </tr>
 *         <tr>
 *             <td><strong>Required</strong></td>
 *             <td>always*</td>
 *             <td>always</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <p>As can be seen, serialization behavior only changes for value types.
 *
 * <h3>Validation: Will a field potentially cause validation failures?</h3>
 *
 * <p>Validation happens at the beginning of serialization and the end of
 * deserialization.  In both cases, the following table describes the impact
 * of a missing field:
 *
 * <table summary="Will a missing field fail validation?" border="1">
 *     <thead>
 *         <tr>
 *             <th></th>
 *             <th>Value Type</th>
 *             <th>Object</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td><strong>Default</strong></td>
 *             <td>no</td>
 *             <td>no</td>
 *         </tr>
 *         <tr>
 *             <td><strong>Optional</strong></td>
 *             <td>no</td>
 *             <td>no</td>
 *         </tr>
 *         <tr>
 *             <td><strong>Required</strong></td>
 *             <td>only on deserialization</td>
 *             <td>yes</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <h2>What does Thrifty do?</h2>
 *
 * <p>For a few reasons, Thrifty has a simpler decision matrix.  As noted, we
 * do not provide server implementations, and so are unconcerned with the
 * distinction between standard and RPC-result serialization.  Second, we
 * do not track {@code isSet} for fields - if a field is {@code null}, it is
 * unset.  As such, we differ from Apache in a few minor ways.
 *
 * <h3>Missing required value-types always fail validation</h3>
 *
 * <p>In Apache, a struct can pass validation if it has required-but-unset
 * value-type fields.  In Thrifty, this is not possible - structs cannot be
 * built without all required members.
 *
 * <h3>Missing, default, value-types are not serialized</h3>
 *
 * <p>This is probably the most significant difference.  Apache will serialize
 * unset value-type fields as whatever default value may be there.  It is
 * technically possible for non-zero values that are not "set" to be sent!
 * In contrast, Thrifty treats default-requiredness "value types" as optional.
 * The rationale for diverging from the official implementation is twofold.
 * One, sending data that is not set is deeply suspect.
 */
public enum Requiredness {
    /**
     * No requirement.  The semantics of this are undefined, but in practice
     * can mean required for serialization, optional for deserialization.
     *
     * <p><strong>This is not consistent between server implementations.</strong>
     *
     * <p>For Thrifty's purposes, this is treated as optional.
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
