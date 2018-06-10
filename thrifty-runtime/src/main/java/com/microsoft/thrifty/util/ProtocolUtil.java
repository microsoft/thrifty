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
package com.microsoft.thrifty.util;

import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.protocol.FieldMetadata;
import com.microsoft.thrifty.protocol.ListMetadata;
import com.microsoft.thrifty.protocol.MapMetadata;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.protocol.SetMetadata;

import java.io.IOException;
import java.net.ProtocolException;

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
            case TType.STRUCT:
                protocol.readStructBegin();
                while (true) {
                    FieldMetadata fieldMetadata = protocol.readFieldBegin();
                    if (fieldMetadata.typeId == TType.STOP) {
                        break;
                    }
                    skip(protocol, fieldMetadata.typeId);
                    protocol.readFieldEnd();
                }
                protocol.readStructEnd();
                break;

            case TType.LIST:
                ListMetadata listMetadata = protocol.readListBegin();
                for (int i = 0; i < listMetadata.size; ++i) {
                    skip(protocol, listMetadata.elementTypeId);
                }
                protocol.readListEnd();
            break;

            case TType.SET:
                SetMetadata setMetadata = protocol.readSetBegin();
                for (int i = 0; i < setMetadata.size; ++i) {
                    skip(protocol, setMetadata.elementTypeId);
                }
                protocol.readSetEnd();
            break;

            case TType.MAP:
                MapMetadata mapMetadata = protocol.readMapBegin();
                for (int i = 0; i < mapMetadata.size; ++i) {
                    skip(protocol, mapMetadata.keyTypeId);
                    skip(protocol, mapMetadata.valueTypeId);
                }
                protocol.readMapEnd();
            break;

            default:
                throw new ProtocolException("Unrecognized TType value: " + typeCode);
        }
    }
}
