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

/**
 * Type constants in the Thrift protocol.
 */
public final class TType {
    public static final byte STOP   = 0;
    public static final byte VOID   = 1;
    public static final byte BOOL   = 2;
    public static final byte BYTE   = 3;
    public static final byte DOUBLE = 4;
    public static final byte I16    = 6;
    public static final byte I32    = 8;
    public static final byte I64    = 10;
    public static final byte STRING = 11;
    public static final byte STRUCT = 12;
    public static final byte MAP    = 13;
    public static final byte SET    = 14;
    public static final byte LIST   = 15;
    public static final byte ENUM   = 16;

    private TType() {
        // no instances
    }
}
