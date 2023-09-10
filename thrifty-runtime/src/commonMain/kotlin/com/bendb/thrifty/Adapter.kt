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
package com.bendb.thrifty

import com.bendb.thrifty.protocol.Protocol
import okio.IOException

/**
 * An object that can read and write a Thrift struct of type [T] from and
 * to a [Protocol] object.
 * @param T the type of struct that can be written and read
 * @param B a [StructBuilder] for [T].
 */
interface Adapter<T, B : StructBuilder<T>> {
    /**
     * Reads a new instance of [T] from the given `protocol`.
     *
     * @param protocol the protocol from which to read
     * @return an instance of [T] populated with the data just read.
     * @throws IOException if reading fails, or if the struct is malformed.
     */
    @Throws(IOException::class)
    fun read(protocol: Protocol): T

    /**
     * Reads a new instane of [T] from the given `protocol`, using
     * the pre-allocated `builder`.
     *
     * @param protocol the protocol from which to read
     * @param builder a builder for [T]
     * @return an instance of [T] populated with the data just read.
     * @throws IOException if reading fails, or if the struct is malformed.
     */
    @Throws(IOException::class)
    fun read(protocol: Protocol, builder: B): T

    /**
     * Writes the given `struct` to the given `protocol`.
     *
     * @param protocol the protocol to which to write the struct
     * @param struct the struct to be written
     * @throws IOException if writing fails
     */
    @Throws(IOException::class)
    fun write(protocol: Protocol, struct: T)
}
