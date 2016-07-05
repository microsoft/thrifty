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
 * A field annotated as {@code {literal @}Redacted} is intended to be obscured
 * in log files, debug statements, etc - either by complete omission, constant
 * value replacement, or value hashing.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Redacted {
    /**
     * Identifies the "strength" of redaction.
     */
    enum Kind {
        /**
         * All values, present or absent, scalar or collection, will
         * be replaced by a constant placeholder string.
         */
        TOTAL,

        /**
         * Scalar values will be hashed, collections will be summarized.
         *
         * <p>Collection summaries consist of the type of collection (e.g.
         * List, Set, Map), the generic parameter, and the collection size.
         */
        OBFUSCATE
    }

    /**
     * Specifies the kind of redaction for the annotated field;
     * defaults to {@link Kind#TOTAL}.
     */
    Kind value() default Kind.TOTAL;
}
