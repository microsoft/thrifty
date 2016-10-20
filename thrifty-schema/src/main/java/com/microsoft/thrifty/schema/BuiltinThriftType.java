package com.microsoft.thrifty.schema;

public class BuiltinThriftType extends NewThriftType {
    BuiltinThriftType(String name) {
        super(name);
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        if (this == NewThriftType.BOOL) {
            return visitor.visitBool(this);
        } else if (this == NewThriftType.BYTE) {
            return visitor.visitByte(this);
        } else if (this == NewThriftType.I8) {
            // Synonym for byte
            return visitor.visitByte(this);
        } else if (this == NewThriftType.I16) {
            return visitor.visitI16(this);
        } else if (this == NewThriftType.I32) {
            return visitor.visitI32(this);
        } else if (this == NewThriftType.I64) {
            return visitor.visitI64(this);
        } else if (this == NewThriftType.DOUBLE) {
            return visitor.visitDouble(this);
        } else if (this == NewThriftType.STRING) {
            return visitor.visitString(this);
        } else if (this == NewThriftType.BINARY) {
            return visitor.visitBinary(this);
        } else {
            throw new AssertionError("Unexpected ThriftType: " + name());
        }
    }
}
