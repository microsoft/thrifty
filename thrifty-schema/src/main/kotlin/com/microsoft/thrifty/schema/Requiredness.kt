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

/**
 * A description of whether and when a field may be absent from a struct,
 * union, or exception.
 *
 *
 * The semantics of required, optional, and the default are underspecified
 * at present.  Intuitively, `required` should mean that the struct containing
 * the field cannot be valid unless the field is present, where `optional`
 * would indicate that the field's presence does not affect the struct's
 * validity.  The actual impacts of these modifiers are both more subtle and
 * implementation-defined, varying both by runtime library and code-generator
 * implementations.
 *
 *
 * For a more thorough treatment of the subject, see
 * [this blog post](http://lionet.livejournal.com/66899.html) which,
 * though being five years old at the time of writing, is still relevant.  Read
 * on for my own interpretation of things.
 *
 * <h2>What does the official implementation do?</h2>
 *
 *
 * There are two times when requiredness modifiers affect behavior - during
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
 * <thead>
 * <tr>
 *   <th></th>
 *   <th>Value types</th>
 *   <th>Objects</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <td>**Default**</td>
 *   <td>always*</td>
 *   <td>if set</td>
 * </tr>
 * <tr>
 *   <td>**Optional**</td>
 *   <td>if set</td>
 *   <td>if set</td>
 * </tr>
 * <tr>
 *   <td>**Required**</td>
 *   <td>always*</td>
 *   <td>always</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * As can be seen, serialization behavior only changes for value types.
 *
 * <h3>Validation: Will a field potentially cause validation failures?</h3>
 *
 *
 * Validation happens at the beginning of serialization and the end of
 * deserialization.  In both cases, the following table describes the impact
 * of a missing field:
 *
 * <table summary="Will a missing field fail validation?" border="1">
 * <thead>
 * <tr>
 * <th></th>
 * <th>Value Type</th>
 * <th>Object</th>
</tr> *
</thead> *
 * <tbody>
 * <tr>
 * <td>**Default**</td>
 * <td>no</td>
 * <td>no</td>
</tr> *
 * <tr>
 * <td>**Optional**</td>
 * <td>no</td>
 * <td>no</td>
</tr> *
 * <tr>
 * <td>**Required**</td>
 * <td>only on deserialization</td>
 * <td>yes</td>
</tr> *
</tbody> *
</table> *
 *
 * <h2>What does Thrifty do?</h2>
 *
 *
 * For a few reasons, Thrifty has a simpler decision matrix.  As noted, we
 * do not provide server implementations, and so are unconcerned with the
 * distinction between standard and RPC-result serialization.  Second, we
 * do not track `isSet` for fields - if a field is `null`, it is
 * unset.  As such, we differ from Apache in a few minor ways.
 *
 * <h3>Missing required value-types always fail validation</h3>
 *
 *
 * In Apache, a struct can pass validation if it has required-but-unset
 * value-type fields.  In Thrifty, this is not possible - structs cannot be
 * built without all required members.
 *
 * <h3>Missing, default, value-types are not serialized</h3>
 *
 *
 * This is probably the most significant difference.  Apache will serialize
 * unset value-type fields as whatever default value may be there.  It is
 * technically possible for non-zero values that are not "set" to be sent!
 * In contrast, Thrifty treats default-requiredness "value types" as optional;
 * sending data that is not intentionally set is deeply suspect.
 */
enum class Requiredness {
    /**
     * No requirement.  The semantics of this are undefined, but in practice
     * can mean required for serialization, optional for deserialization.
     *
     *
     * **This is not consistent between server implementations.**
     *
     *
     * For Thrifty's purposes, this is treated as optional.
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