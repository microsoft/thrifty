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
        if (this.equals(NewThriftType.BOOL)) {
            return visitor.visitBool(this);
        } else if (this.equals(NewThriftType.BYTE)) {
            return visitor.visitByte(this);
        } else if (this.equals(NewThriftType.I8)) {
            // Synonym for byte
            return visitor.visitByte(this);
        } else if (this.equals(NewThriftType.I16)) {
            return visitor.visitI16(this);
        } else if (this.equals(NewThriftType.I32)) {
            return visitor.visitI32(this);
        } else if (this.equals(NewThriftType.I64)) {
            return visitor.visitI64(this);
        } else if (this.equals(NewThriftType.DOUBLE)) {
            return visitor.visitDouble(this);
        } else if (this.equals(NewThriftType.STRING)) {
            return visitor.visitString(this);
        } else if (this.equals(NewThriftType.BINARY)) {
            return visitor.visitBinary(this);
        } else {
            throw new AssertionError("Unexpected ThriftType: " + name());
        }
    }
}
