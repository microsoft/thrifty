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

import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.util.ProtocolUtil.skip
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Represents a Thrift protocol-level error.
 */
class ThriftException(
        @JvmField
        val kind: Kind,
        message: String?) : RuntimeException(message), Struct {
    /**
     * Identifies kinds of protocol violation.
     */
    enum class Kind(val value: Int) {
        /**
         * An unspecified or unexpected error.
         */
        UNKNOWN(0),

        /**
         * The server does not understand the message received.
         */
        UNKNOWN_METHOD(1),

        /**
         * The message type ID was not expected.  This would indicate
         * a bug in generated code.
         */
        INVALID_MESSAGE_TYPE(2),

        /**
         * The server replied with an unexpected method name for the given
         * sequence ID.  Indicates a probable client-side bug.
         */
        WRONG_METHOD_NAME(3),

        /**
         * The server replied with an unrecognized sequence ID.  Indicates
         * a probable client-side bug.
         */
        BAD_SEQUENCE_ID(4),

        /**
         * The server reply did not contain the expected result.  Indicates
         * a probable schema-version mismatch, or a server bug.
         */
        MISSING_RESULT(5),

        /**
         * The server experienced an unexpected error.
         */
        INTERNAL_ERROR(6), PROTOCOL_ERROR(7), INVALID_TRANSFORM(8), INVALID_PROTOCOL(9), UNSUPPORTED_CLIENT_TYPE(10);

        companion object {
            @JvmStatic
            fun findByValue(value: Int): Kind {
                return values().firstOrNull { it.value == value } ?: UNKNOWN
            }
        }
    }

    override fun write(protocol: Protocol) {
        protocol.writeStructBegin("TApplicationException")
        if (message != null) {
            protocol.writeFieldBegin("message", 1, TType.STRING)
            protocol.writeString(message)
            protocol.writeFieldEnd()
        }
        protocol.writeFieldBegin("type", 2, TType.I32)
        protocol.writeI32(kind.value)
        protocol.writeFieldEnd()
        protocol.writeFieldStop()
        protocol.writeStructEnd()
    }

    companion object {
        @JvmStatic
        fun read(protocol: Protocol): ThriftException {
            var message: String? = null
            var kind = Kind.UNKNOWN
            protocol.readStructBegin()
            while (true) {
                val field = protocol.readFieldBegin()
                if (field.typeId == TType.STOP) {
                    break
                }
                when (field.fieldId) {
                    1.toShort() ->
                        if (field.typeId == TType.STRING) {
                            message = protocol.readString()
                        } else {
                            skip(protocol, field.typeId)
                        }
                    2.toShort() ->
                        if (field.typeId == TType.I32) {
                            kind = Kind.findByValue(protocol.readI32())
                        } else {
                            skip(protocol, field.typeId)
                        }
                    else -> skip(protocol, field.typeId)
                }
                protocol.readFieldEnd()
            }
            protocol.readStructEnd()
            return ThriftException(kind, message)
        }
    }
}
