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
package com.microsoft.thrifty

/**
 * Type constants in the Thrift protocol.
 */
object TType {
    const val STOP: Byte = 0
    const val VOID: Byte = 1
    const val BOOL: Byte = 2
    const val BYTE: Byte = 3
    const val DOUBLE: Byte = 4
    const val I16: Byte = 6
    const val I32: Byte = 8
    const val I64: Byte = 10
    const val STRING: Byte = 11
    const val STRUCT: Byte = 12
    const val MAP: Byte = 13
    const val SET: Byte = 14
    const val LIST: Byte = 15
}