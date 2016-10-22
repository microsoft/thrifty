package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableMap;

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
    public static final BuiltinThriftType VOID   = new BuiltinThriftType("void");

    private static final ImmutableMap<String, NewThriftType> BUILTINS;

    static {
        BUILTINS = ImmutableMap.<String, NewThriftType>builder()
                .put(BOOL.name(),   BOOL)
                .put(BYTE.name(),   BYTE)
                .put(I8.name(),     I8)
                .put(I16.name(),    I16)
                .put(I32.name(),    I32)
                .put(I64.name(),    I64)
                .put(DOUBLE.name(), DOUBLE)
                .put(STRING.name(), STRING)
                .put(BINARY.name(), BINARY)
                .put(VOID.name(),   VOID)
                .build();
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewThriftType that = (NewThriftType) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
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

        T visitStruct(NewStructType structType);
    }
}
