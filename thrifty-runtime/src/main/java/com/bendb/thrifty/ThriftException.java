/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty;

import com.bendb.thrifty.protocol.FieldMetadata;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.util.ProtocolUtil;

import java.io.IOException;

/**
 * Represents a Thrift protocol-level error.
 */
public class ThriftException extends RuntimeException {
    /**
     * Identifies kinds of protocol violation.
     */
    public enum Kind {
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
        INTERNAL_ERROR(6),

        PROTOCOL_ERROR(7),
        INVALID_TRANSFORM(8),
        INVALID_PROTOCOL(9),
        UNSUPPORTED_CLIENT_TYPE(10);

        final int value;

        Kind(int value) {
            this.value = value;
        }

        static Kind findByValue(int value) {
            for (Kind kind : values()) {
                if (kind.value == value) {
                    return kind;
                }
            }
            return UNKNOWN;
        }
    }

    public final Kind kind;

    public ThriftException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public static ThriftException read(Protocol protocol) throws IOException {
        String message = null;
        Kind kind = Kind.UNKNOWN;

        protocol.readStructBegin();
        while (true) {
            FieldMetadata field = protocol.readFieldBegin();
            if (field.typeId == TType.STOP) {
                break;
            }

            switch (field.fieldId) {
                case 1:
                    if (field.typeId == TType.STRING) {
                        message = protocol.readString();
                    } else {
                        ProtocolUtil.skip(protocol, field.typeId);
                    }
                    break;
                case 2:
                    if (field.typeId == TType.I32) {
                        kind = Kind.findByValue(protocol.readI32());
                    } else {
                        ProtocolUtil.skip(protocol, field.typeId);
                    }
                    break;
                default:
                    ProtocolUtil.skip(protocol, field.typeId);
                    break;
            }
            protocol.readFieldEnd();
        }
        protocol.readStructEnd();

        return new ThriftException(kind, message);
    }
}
