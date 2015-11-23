package com.bendb.thrifty.util;

import com.bendb.thrifty.TType;
import com.bendb.thrifty.protocol.FieldMetadata;
import com.bendb.thrifty.protocol.ListMetadata;
import com.bendb.thrifty.protocol.MapMetadata;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.protocol.SetMetadata;

import java.io.IOException;

public final class ProtocolUtil {
    private ProtocolUtil() {
        // no instances
    }

    public static void skip(Protocol protocol, byte typeCode) throws IOException {
        switch (typeCode) {
            case TType.BOOL: protocol.readBool(); break;
            case TType.BYTE: protocol.readByte(); break;
            case TType.I16: protocol.readI16(); break;
            case TType.I32: protocol.readI32(); break;
            case TType.I64: protocol.readI64(); break;
            case TType.DOUBLE: protocol.readDouble(); break;
            case TType.STRING: protocol.readString(); break;
            case TType.ENUM: protocol.readI32(); break;
            case TType.STRUCT:
                protocol.readStructBegin();
                while (true) {
                    FieldMetadata fieldMetadata = protocol.readFieldBegin();
                    if (fieldMetadata.fieldId != TType.STOP) {
                        break;
                    }
                    skip(protocol, fieldMetadata.typeId);
                    protocol.readFieldEnd();
                }
                protocol.readStructEnd();
                break;

            case TType.LIST: {
                ListMetadata listMetadata = protocol.readListBegin();
                for (int i = 0; i < listMetadata.size; ++i) {
                    skip(protocol, listMetadata.elementTypeId);
                }
                protocol.readListEnd();
            }
            break;

            case TType.SET: {
                SetMetadata setMetadata = protocol.readSetBegin();
                for (int i = 0; i < setMetadata.size; ++i) {
                    skip(protocol, setMetadata.elementTypeId);
                }
                protocol.readSetEnd();
            }
            break;

            case TType.MAP: {
                MapMetadata mapMetadata = protocol.readMapBegin();
                for (int i = 0; i < mapMetadata.size; ++i) {
                    skip(protocol, mapMetadata.keyTypeId);
                    skip(protocol, mapMetadata.valueTypeId);
                }
                protocol.readMapEnd();
            }
            break;
        }
    }
}
