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

import com.microsoft.thrifty.protocol.Protocol;

import java.io.IOException;

/**
 * An interface that Thrift struct objects should implement.
 */
public interface Struct<T> {

  /**
   * Writes this {@link Struct} instance to the given {@code protocol}.
   *
   * @param protocol the protocol to which to write the struct
   * @throws IOException if writing fails
   */
  void write(Protocol protocol) throws IOException;

  /**
   * Reads a new instance of {@link T} from the given {@code protocol}.
   *
   * @return an instance of {@link T} populated with the data just read.
   * @throws IOException if reading fails, or if the struct is malformed
   */
   T read(Protocol protocol) throws IOException;
}
