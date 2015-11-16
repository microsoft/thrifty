package com.bendb.thrifty;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a type name and all containing namespaces.
 */
public abstract class ThriftType {
    private static final String LIST_PREFIX = "list<";
    private static final String SET_PREFIX = "set<";
    private static final String MAP_PREFIX = "map<";

    public static final ThriftType BOOL = new BuiltinType("bool");
    public static final ThriftType BYTE = new BuiltinType("byte");
    public static final ThriftType I16 = new BuiltinType("i16");
    public static final ThriftType I32 = new BuiltinType("i32");
    public static final ThriftType I64 = new BuiltinType("i64");
    public static final ThriftType DOUBLE = new BuiltinType("double");
    public static final ThriftType STRING = new BuiltinType("string");
    public static final ThriftType BINARY = new BuiltinType("binary");

    private static final ImmutableMap<String, ThriftType> BUILTINS;
    static {
        ImmutableMap.Builder<String, ThriftType> builtins = ImmutableMap.builder();
        builtins.put(BOOL.name, BOOL);
        builtins.put(BYTE.name, BYTE);
        builtins.put(I16.name, I16);
        builtins.put(I32.name, I32);
        builtins.put(I64.name, I64);
        builtins.put(DOUBLE.name, DOUBLE);
        builtins.put(STRING.name, STRING);
        builtins.put(BINARY.name, BINARY);
        BUILTINS = builtins.build();
    }

    private final String name;
    private final Map<NamespaceScope, String> namespaces = new LinkedHashMap<>();

    protected ThriftType(String name) {
        this.name = name;
    }

    /**
     * Gets a {@link ThriftType} for the given type name.
     *
     * Preconditions:
     * The given name is non-null, non-empty, and is the product
     * of ThriftParser.  In particular, it is assumed that collection
     * types are already validated.
     *
     * @param name
     * @return
     */
    public static ThriftType get(String name) {
        ThriftType t = BUILTINS.get(name);
        if (t != null) {
            return t;
        }

        if (name.startsWith(LIST_PREFIX)) {
            String elementTypeName = name
                    .substring(LIST_PREFIX.length(), name.indexOf('>'))
                    .trim();
            ThriftType elementType = ThriftType.get(elementTypeName);
            return new ListType(name, elementType);
        } else if (name.startsWith(SET_PREFIX)) {
            String elementTypeName = name
                    .substring(SET_PREFIX.length(), name.indexOf('>'))
                    .trim();
            ThriftType elementType = ThriftType.get(elementTypeName);
            return new SetType(name, elementType);
        } else if (name.startsWith("map<")) {
            String[] bothTypeNames = name
                    .substring(MAP_PREFIX.length(), name.indexOf('>'))
                    .trim()
                    .split(",");
            if (bothTypeNames.length != 2) {
                throw new AssertionError("Parser error - invalid map-type name: " + name);
            }
            ThriftType keyType = ThriftType.get(bothTypeNames[0]);
            ThriftType valueType = ThriftType.get(bothTypeNames[1]);
            return new MapType(name, keyType, valueType);

        } else {
            return new UserType(name);
        }
    }

    public static ThriftType typedefOf(ThriftType oldType, String name) {
        if (BUILTINS.get(name) != null) {
            throw new IllegalArgumentException("Cannot typedef built-in type: " + name);
        }
        return new TypedefType(name, oldType);
    }

    public String name() {
        return this.name;
    }

    public boolean isBuiltin() {
        return false;
    }

    public boolean isTypedef() {
        return false;
    }

    public boolean isContainer() {
        ThriftType t = this;
        while (t instanceof TypedefType) {
            t = ((TypedefType) t).originalType;
        }
        return t instanceof ListType
                || t instanceof SetType
                || t instanceof MapType;
    }

    public void addNamespace(NamespaceScope scope, String namespace) {
        String oldNamespace = namespaces.put(scope, namespace);
        if (oldNamespace != null) {
            namespaces.put(scope, oldNamespace);
            throw new IllegalArgumentException(
                    "duplicated namespaces, type " + name + " already in ns " + oldNamespace);
        }
    }

    public String getNamespace(NamespaceScope scope) {
        String ns = namespaces.get(scope);
        if (ns == null) {
            ns = namespaces.get(NamespaceScope.ALL);
        }
        return ns;
    }

    private static class BuiltinType extends ThriftType {
        BuiltinType(String name) {
            super(name);
        }

        @Override
        public boolean isBuiltin() {
            return true;
        }
    }

    private static class UserType extends ThriftType {
        UserType(String name) {
            super(name);
        }
    }

    private static class ListType extends ThriftType {
        private final ThriftType elementType;

        ListType(String name, ThriftType elementType) {
            super(name);
            this.elementType = elementType;
        }

        public ThriftType elementType() {
            return elementType;
        }
    }

    private static class SetType extends ThriftType {
        private final ThriftType elementType;

        SetType(String name, ThriftType elementType) {
            super(name);
            this.elementType = elementType;
        }

        public ThriftType elementType() {
            return elementType;
        }
    }

    private static class MapType extends ThriftType {
        private final ThriftType keyType;
        private final ThriftType valueType;

        MapType(String name, ThriftType keyType, ThriftType valueType) {
            super(name);
            this.keyType = keyType;
            this.valueType = valueType;
        }

        public ThriftType keyType() {
            return keyType;
        }

        public ThriftType valueType() {
            return valueType;
        }
    }

    private static class TypedefType extends ThriftType {
        private final ThriftType originalType;

        TypedefType(String name, ThriftType originalType) {
            super(name);
            this.originalType = originalType;
        }

        @Override
        public boolean isTypedef() {
            return true;
        }

        public ThriftType originalType() {
            return originalType;
        }
    }
}
