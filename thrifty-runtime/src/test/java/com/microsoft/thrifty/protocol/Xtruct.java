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
package com.microsoft.thrifty.protocol;

import com.microsoft.thrifty.Adapter;
import com.microsoft.thrifty.StructBuilder;
import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.ThriftField;
import com.microsoft.thrifty.util.ProtocolUtil;

import java.io.IOException;

public final class Xtruct {
    public static final Adapter<Xtruct, Builder> ADAPTER = new XtructAdapter();

    @ThriftField(
            fieldId = 1,
            isRequired = false
    )
    public final String string_thing;

    @ThriftField(
            fieldId = 4,
            isRequired = false
    )
    public final Byte byte_thing;

    @ThriftField(
            fieldId = 9,
            isRequired = false
    )
    public final Integer i32_thing;

    @ThriftField(
            fieldId = 11,
            isRequired = false
    )
    public final Long i64_thing;

    @ThriftField(
            fieldId = 13,
            isRequired = false
    )
    public final Double double_thing;

    private Xtruct(Builder builder) {
        this.string_thing = builder.string_thing;
        this.byte_thing = builder.byte_thing;
        this.i32_thing = builder.i32_thing;
        this.i64_thing = builder.i64_thing;
        this.double_thing = builder.double_thing;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (!(other instanceof Xtruct)) return false;
        Xtruct that = (Xtruct) other;
        return (this.string_thing == that.string_thing || (this.string_thing != null && this.string_thing.equals(that.string_thing)))
                && (this.byte_thing == that.byte_thing || (this.byte_thing != null && this.byte_thing.equals(that.byte_thing)))
                && (this.i32_thing == that.i32_thing || (this.i32_thing != null && this.i32_thing.equals(that.i32_thing)))
                && (this.i64_thing == that.i64_thing || (this.i64_thing != null && this.i64_thing.equals(that.i64_thing)))
                && (this.double_thing == that.double_thing || (this.double_thing != null && this.double_thing.equals(that.double_thing)));
    }

    @Override
    public int hashCode() {
        int code = 16777619;
        code ^= (this.string_thing == null) ? 0 : this.string_thing.hashCode();
        code *= 0x811c9dc5;
        code ^= (this.byte_thing == null) ? 0 : this.byte_thing.hashCode();
        code *= 0x811c9dc5;
        code ^= (this.i32_thing == null) ? 0 : this.i32_thing.hashCode();
        code *= 0x811c9dc5;
        code ^= (this.i64_thing == null) ? 0 : this.i64_thing.hashCode();
        code *= 0x811c9dc5;
        code ^= (this.double_thing == null) ? 0 : this.double_thing.hashCode();
        code *= 0x811c9dc5;
        return code;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Xtruct").append("{\n  ");
        sb.append("string_thing=");
        sb.append(this.string_thing == null ? "null" : this.string_thing);
        sb.append(",\n  ");
        sb.append("byte_thing=");
        sb.append(this.byte_thing == null ? "null" : this.byte_thing);
        sb.append(",\n  ");
        sb.append("i32_thing=");
        sb.append(this.i32_thing == null ? "null" : this.i32_thing);
        sb.append(",\n  ");
        sb.append("i64_thing=");
        sb.append(this.i64_thing == null ? "null" : this.i64_thing);
        sb.append(",\n  ");
        sb.append("double_thing=");
        sb.append(this.double_thing == null ? "null" : this.double_thing);
        sb.append("\n}");
        return sb.toString();
    }

    public static final class Builder implements StructBuilder<Xtruct> {
        private String string_thing;

        private Byte byte_thing;

        private Integer i32_thing;

        private Long i64_thing;

        private Double double_thing;

        public Builder() {
        }

        public Builder(Xtruct struct) {
            this.string_thing = struct.string_thing;
            this.byte_thing = struct.byte_thing;
            this.i32_thing = struct.i32_thing;
            this.i64_thing = struct.i64_thing;
            this.double_thing = struct.double_thing;
        }

        public Builder string_thing(String string_thing) {
            this.string_thing = string_thing;
            return this;
        }

        public Builder byte_thing(Byte byte_thing) {
            this.byte_thing = byte_thing;
            return this;
        }

        public Builder i32_thing(Integer i32_thing) {
            this.i32_thing = i32_thing;
            return this;
        }

        public Builder i64_thing(Long i64_thing) {
            this.i64_thing = i64_thing;
            return this;
        }

        public Builder double_thing(Double double_thing) {
            this.double_thing = double_thing;
            return this;
        }

        @Override
        public Xtruct build() {
            return new Xtruct(this);
        }

        @Override
        public void reset() {
            this.string_thing = null;
            this.byte_thing = null;
            this.i32_thing = null;
            this.i64_thing = null;
            this.double_thing = null;
        }
    }

    private static final class XtructAdapter implements Adapter<Xtruct, Builder> {
        @Override
        public void write(Protocol protocol, Xtruct struct) throws IOException {
            protocol.writeStructBegin("Xtruct");
            if (struct.string_thing != null) {
                protocol.writeFieldBegin("string_thing", 1, TType.STRING);
                protocol.writeString(struct.string_thing);
                protocol.writeFieldEnd();
            }
            if (struct.byte_thing != null) {
                protocol.writeFieldBegin("byte_thing", 4, TType.BYTE);
                protocol.writeByte(struct.byte_thing);
                protocol.writeFieldEnd();
            }
            if (struct.i32_thing != null) {
                protocol.writeFieldBegin("i32_thing", 9, TType.I32);
                protocol.writeI32(struct.i32_thing);
                protocol.writeFieldEnd();
            }
            if (struct.i64_thing != null) {
                protocol.writeFieldBegin("i64_thing", 11, TType.I64);
                protocol.writeI64(struct.i64_thing);
                protocol.writeFieldEnd();
            }
            if (struct.double_thing != null) {
                protocol.writeFieldBegin("double_thing", 13, TType.DOUBLE);
                protocol.writeDouble(struct.double_thing);
                protocol.writeFieldEnd();
            }
            protocol.writeFieldStop();
            protocol.writeStructEnd();
        }

        @Override
        public Xtruct read(Protocol protocol, Builder builder) throws IOException {
            protocol.readStructBegin();
            while (true) {
                FieldMetadata field = protocol.readFieldBegin();
                if (field.typeId == TType.STOP) {
                    break;
                }
                switch (field.fieldId) {
                    case 1: {
                        if (field.typeId == TType.STRING) {
                            String value = protocol.readString();
                            builder.string_thing(value);
                        } else {
                            ProtocolUtil.skip(protocol, field.typeId);
                        }
                    }
                    break;
                    case 4: {
                        if (field.typeId == TType.BYTE) {
                            byte value = protocol.readByte();
                            builder.byte_thing(value);
                        } else {
                            ProtocolUtil.skip(protocol, field.typeId);
                        }
                    }
                    break;
                    case 9: {
                        if (field.typeId == TType.I32) {
                            int value = protocol.readI32();
                            builder.i32_thing(value);
                        } else {
                            ProtocolUtil.skip(protocol, field.typeId);
                        }
                    }
                    break;
                    case 11: {
                        if (field.typeId == TType.I64) {
                            long value = protocol.readI64();
                            builder.i64_thing(value);
                        } else {
                            ProtocolUtil.skip(protocol, field.typeId);
                        }
                    }
                    break;
                    case 13: {
                        if (field.typeId == TType.DOUBLE) {
                            double value = protocol.readDouble();
                            builder.double_thing(value);
                        } else {
                            ProtocolUtil.skip(protocol, field.typeId);
                        }
                    }
                    break;
                    default: {
                        ProtocolUtil.skip(protocol, field.typeId);
                    }
                    break;
                }
                protocol.readFieldEnd();
            }
            return builder.build();
        }

        @Override
        public Xtruct read(Protocol protocol) throws IOException {
            return read(protocol, new Builder());
        }
    }
}

