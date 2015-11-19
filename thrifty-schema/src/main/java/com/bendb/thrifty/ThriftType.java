package com.bendb.thrifty;

import com.google.common.base.Objects;
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

    /**
     * Represents a type that has not yet been resolved.
     *
     * Not an actual type - used for type resolution algorithms only.
     */
    static final ThriftType PLACEHOLDER = new BuiltinType("placeholder");

    public static final ThriftType BOOL = new BuiltinType("bool");
    public static final ThriftType BYTE = new BuiltinType("byte");
    public static final ThriftType I16 = new BuiltinType("i16");
    public static final ThriftType I32 = new BuiltinType("i32");
    public static final ThriftType I64 = new BuiltinType("i64");
    public static final ThriftType DOUBLE = new BuiltinType("double");
    public static final ThriftType STRING = new BuiltinType("string");
    public static final ThriftType BINARY = new BuiltinType("binary");

    // Only valid as a "return type" for service methods
    public static final ThriftType VOID = new BuiltinType("void");

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
        } else if (name.startsWith(MAP_PREFIX)) {
            String[] bothTypeNames = name
                    .substring(MAP_PREFIX.length(), name.indexOf('>'))
                    .trim()
                    .split(",");
            if (bothTypeNames.length != 2) {
                throw new AssertionError("Parser error - invalid map-type name: " + name);
            }
            ThriftType keyType = ThriftType.get(bothTypeNames[0].trim());
            ThriftType valueType = ThriftType.get(bothTypeNames[1].trim());
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

    public boolean isList() {
        return getTrueType() instanceof ListType;
    }

    public boolean isSet() {
        return getTrueType() instanceof SetType;
    }

    public boolean isMap() {
        return getTrueType() instanceof MapType;
    }

    public ThriftType getTrueType() {
        ThriftType t = this;
        while (t instanceof TypedefType) {
            t = ((TypedefType) t).originalType;
        }
        return t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThriftType that = (ThriftType) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
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

    static class UserType extends ThriftType {
        UserType(String name) {
            super(name);
        }
    }

    static class ListType extends ThriftType {
        private final ThriftType elementType;

        ListType(String name, ThriftType elementType) {
            super(name);
            this.elementType = elementType;
        }

        public ThriftType elementType() {
            return elementType;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o)
                    && elementType.equals(((ListType) o).elementType);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(super.hashCode(), elementType.hashCode());
        }
    }

    static class SetType extends ThriftType {
        private final ThriftType elementType;

        SetType(String name, ThriftType elementType) {
            super(name);
            this.elementType = elementType;
        }

        public ThriftType elementType() {
            return elementType;
        }


        @Override
        public boolean equals(Object o) {
            return super.equals(o)
                    && elementType.equals(((SetType) o).elementType);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(super.hashCode(), elementType.hashCode());
        }
    }

    static class MapType extends ThriftType {
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

        @Override
        public boolean equals(Object o) {
            if (super.equals(o)) {
                MapType that = (MapType) o;
                return this.keyType.equals(that.keyType)
                        && this.valueType.equals(that.valueType);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(
                    super.hashCode(),
                    keyType.hashCode(),
                    valueType.hashCode());
        }
    }

    static class TypedefType extends ThriftType {
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
