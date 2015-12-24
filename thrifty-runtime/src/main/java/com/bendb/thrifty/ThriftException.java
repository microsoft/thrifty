package com.bendb.thrifty;

import com.bendb.thrifty.protocol.FieldMetadata;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.util.ProtocolUtil;

import java.io.IOException;

/**
 * Represents
 */
public class ThriftException extends RuntimeException {
    public enum Kind {
        UNKNOWN(0),
        UNKNOWN_METHOD(1),
        INVALID_MESSAGE_TYPE(2),
        WRONG_METHOD_NAME(3),
        BAD_SEQUENCE_ID(4),
        MISSING_RESULT(5),
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
