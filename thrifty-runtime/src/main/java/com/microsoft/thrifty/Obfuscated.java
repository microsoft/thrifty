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
package com.microsoft.thrifty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field annotated as {@code Obfuscated} is intended to be obscured in log
 * files, debug statements, etc.  The printed value of an obfuscated field is
 * dependent on its type.  Scalar values (byte, short, int, etc) will be hashed
 * using an unspecified digest, and collection types will be summarized.
 *
 * <p>Obfuscated is a suitable choice when one wishes to avoid logging
 * personally-identifiable information, but still wishes to distinguish one
 * value from another.
 *
 * <p>Collection summaries consist of the collection kind (List, Set, or Map),
 * its generic parameter(s), and the number of elements in the collection.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Obfuscated {
}
