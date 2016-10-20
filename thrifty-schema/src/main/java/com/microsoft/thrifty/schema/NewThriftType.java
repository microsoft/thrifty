package com.microsoft.thrifty.schema;

public abstract class NewThriftType {
    public static final BuiltinThriftType BOOL   = new BuiltinThriftType("bool");
    public static final BuiltinThriftType BYTE   = new BuiltinThriftType("byte");
    public static final BuiltinThriftType I8     = new BuiltinThriftType("i8");
    public static final BuiltinThriftType I16    = new BuiltinThriftType("i16");
    public static final BuiltinThriftType I32    = new BuiltinThriftType("i32");
    public static final BuiltinThriftType I64    = new BuiltinThriftType("i64");
    public static final BuiltinThriftType DOUBLE = new BuiltinThriftType("double");
    public static final BuiltinThriftType STRING = new BuiltinThriftType("string");
    public static final BuiltinThriftType BINARY = new BuiltinThriftType("binary");

    private final String name;

    NewThriftType(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract <T> T accept(Visitor<T> visitor);

    public boolean isBuiltin() {
        return false;
    }

    public boolean isList() {
        return false;
    }

    public boolean isSet() {
        return false;
    }

    public boolean isMap() {
        return false;
    }

    public boolean isEnum() {
        return false;
    }

    public boolean isStruct() {
        return false;
    }

    public boolean isService() {
        return false;
    }

    public interface Visitor<T> {
        T visitBool(BuiltinThriftType boolType);
        T visitByte(BuiltinThriftType byteType);
        T visitI16(BuiltinThriftType i16Type);
        T visitI32(BuiltinThriftType i32Type);
        T visitI64(BuiltinThriftType i64Type);
        T visitDouble(BuiltinThriftType doubleType);
        T visitString(BuiltinThriftType stringType);
        T visitBinary(BuiltinThriftType binaryType);

        T visitEnum(NewEnumType enumType);

        T visitList(ListType listType);

        T visitSet(SetType setType);

        T visitMap(MapType mapType);
    }
}
