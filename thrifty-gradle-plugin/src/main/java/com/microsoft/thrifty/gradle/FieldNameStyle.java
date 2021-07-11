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
package com.microsoft.thrifty.gradle;

/**
 * The name styles supported by Thrifty.
 *
 * <table>
 *     <thead>
 *         <th>Name</th>
 *         <th>Description</th>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>DEFAULT</td>
 *             <td>Field names are exactly as in .thrift IDL</td>
 *         </tr>
 *         <tr>
 *             <td>JAVA</td>
 *             <td>Field names are lowerCamelCase, beginning with a lower-cased letter.</td>
 *         </tr>
 *         <tr>
 *             <td>PASCAL</td>
 *             <td>Field names are UpperCamelCase, beginning with an upper-cased letter.</td>
 *         </tr>
 *     </tbody>
 * </table>
 */
public enum FieldNameStyle {
    DEFAULT,
    JAVA,
    PASCAL
}
