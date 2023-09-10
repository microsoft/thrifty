/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.internal

actual class AtomicBoolean actual constructor(
    initialValue: Boolean
) {
    private val actualAtomicBool = kotlin.concurrent.AtomicInt(if (initialValue) 1 else 0)

    actual fun get(): Boolean {
        return actualAtomicBool.value == 1
    }

    actual fun compareAndSet(expected: Boolean, update: Boolean): Boolean {
        val expectedNum = if (expected) 1 else 0
        val updateNum = if (update) 1 else 0
        return actualAtomicBool.compareAndSet(expectedNum, updateNum)
    }
}

actual class AtomicInteger actual constructor(
    initialValue: Int
) {
    private val actualAtomicInt = kotlin.concurrent.AtomicInt(initialValue)

    actual fun get(): Int = actualAtomicInt.value

    actual fun incrementAndGet(): Int = actualAtomicInt.incrementAndGet()
}
