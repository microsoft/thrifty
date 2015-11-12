package com.bendb.thrifty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a type name and all containing namespaces.
 */
public final class ThriftType {
    public static final ThriftType BOOL = new ThriftType(true, "bool");
    public static final ThriftType BYTE = new ThriftType(true, "byte");
    public static final ThriftType I16 = new ThriftType(true, "i16");
    public static final ThriftType I32 = new ThriftType(true, "i32");
    public static final ThriftType I64 = new ThriftType(true, "i64");
    public static final ThriftType DOUBLE = new ThriftType(true, "double");
    public static final ThriftType STRING = new ThriftType(true, "string");
    public static final ThriftType BINARY = new ThriftType(true, "binary");

    private static final Map<String, ThriftType> BUILTINS;
    static {
        Map<String, ThriftType> builtins = new LinkedHashMap<>();
        builtins.put(BOOL.name, BOOL);
        builtins.put(BYTE.name, BYTE);
        builtins.put(I16.name, I16);
        builtins.put(I32.name, I32);
        builtins.put(I64.name, I64);
        builtins.put(DOUBLE.name, DOUBLE);
        builtins.put(STRING.name, STRING);
        builtins.put(BINARY.name, BINARY);
        BUILTINS = builtins;
    }

    private final boolean isBuiltin;
    private final String name;
    private final Map<NamespaceScope, String> namespaces = new LinkedHashMap<>();

    private ThriftType(boolean isBuiltin, String name) {
        this.isBuiltin = isBuiltin;
        this.name = name;
    }

    public static ThriftType get(String name) {
        ThriftType t = BUILTINS.get(name);
        if (t != null) {
            return t;
        }
        return new ThriftType(false, name);
    }

    public String name() {
        return this.name;
    }

    public boolean isBuiltin() {
        return this.isBuiltin;
    }


}
