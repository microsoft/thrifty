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
package com.microsoft.thrifty.schema;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Represents a type name and all containing namespaces.
 */
@SuppressWarnings("StaticInitializerReferencesSubClass") // Safe here because we don't lock on any static data
public abstract class ThriftType {
    private static final String LIST_PREFIX = "list<";
    private static final String SET_PREFIX = "set<";
    private static final String MAP_PREFIX = "map<";

    public static final ThriftType BOOL = new BuiltinType("bool");
    public static final ThriftType BYTE = new BuiltinType("byte");
    public static final ThriftType I8 = new BuiltinType("i8");
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
        builtins.put(I8.name, I8);
        builtins.put(I16.name, I16);
        builtins.put(I32.name, I32);
        builtins.put(I64.name, I64);
        builtins.put(DOUBLE.name, DOUBLE);
        builtins.put(STRING.name, STRING);
        builtins.put(BINARY.name, BINARY);
        BUILTINS = builtins.build();
    }

    /**
     * Merge two maps, with keys in {@code newAnnotations} taking precedence
     * over keys in {@code baseAnnotations}.
     */
    private static <K, V> ImmutableMap<K, V> merge(
            Map<K, V> baseAnnotations,
            Map<K, V> newAnnotations) {
        Map<K, V> map = new HashMap<>(baseAnnotations);
        map.putAll(newAnnotations);
        return ImmutableMap.copyOf(map);
    }

    private final String name;
    private final ImmutableMap<String, String> annotations;

    private ThriftType(String name) {
        this(name, ImmutableMap.<String, String>of());
    }

    private ThriftType(String name, Map<String, String> annotations) {
        this.name = name;
        this.annotations = ImmutableMap.copyOf(annotations);
    }

    public static ThriftType list(ThriftType elementType) {
        return list(elementType, ImmutableMap.<String, String>of());
    }

    public static ThriftType list(ThriftType elementType, Map<String, String> annotations) {
        String name = LIST_PREFIX + elementType.name + ">";
        return new ListType(name, elementType, annotations);
    }

    public static ThriftType set(ThriftType elementType) {
        return set(elementType, ImmutableMap.<String, String>of());
    }

    public static ThriftType set(ThriftType elementType, Map<String, String> annotations) {
        String name = SET_PREFIX + elementType.name + ">";
        return new SetType(name, elementType, annotations);
    }

    public static ThriftType map(ThriftType keyType, ThriftType valueType) {
        return map(keyType, valueType, ImmutableMap.<String, String>of());
    }

    public static ThriftType map(ThriftType keyType, ThriftType valueType, Map<String, String> annotations) {
        String name = MAP_PREFIX + keyType.name + "," + valueType.name + ">";
        return new MapType(name, keyType, valueType, annotations);
    }

    /**
     * Gets a {@link ThriftType} for the given type name.
     *
     * Preconditions:
     * The given name is non-null, non-empty, and is the product
     * of ThriftParser.  In particular, it is assumed that collection
     * types are already validated.
     *
     * @param name the name of the type.
     * @param namespaces all defined namespaces for this type.
     * @return a {@link ThriftType} representing the given typename.
     */
    public static ThriftType get(@Nonnull String name, Map<NamespaceScope, String> namespaces) {
        return get(name, namespaces, ImmutableMap.<String, String>of());
    }

    public static ThriftType get(
            @Nonnull String name,
            Map<NamespaceScope, String> namespaces,
            Map<String, String> annotations) {
        ThriftType t = BUILTINS.get(name);
        if (t != null && annotations.isEmpty()) {
            return t;
        } else if (t != null) {
            return new BuiltinType(name, annotations);
        }

        return new UserType(name, namespaces, false, annotations);
    }

    public static ThriftType enumType(@Nonnull String name, Map<NamespaceScope, String> namespaces) {
        return enumType(name, namespaces, ImmutableMap.<String, String>of());
    }

    public static ThriftType enumType(
            @Nonnull String name,
            Map<NamespaceScope, String> namespaces,
            Map<String, String> annotations) {
        return new UserType(name, namespaces, true, annotations);
    }
    public static ThriftType typedefOf(ThriftType oldType,
            String name,
            Map<NamespaceScope, String> namespaces) {
        return typedefOf(oldType, name, namespaces, ImmutableMap.<String, String>of());
    }

    public static ThriftType typedefOf(ThriftType oldType,
            String name,
            Map<NamespaceScope, String> namespaces,
            Map<String, String> annotations) {
        if (BUILTINS.get(name) != null) {
            throw new IllegalArgumentException("Cannot redefine built-in type: " + name);
        }
        return new TypedefType(name, oldType, namespaces, annotations);
    }

    public String name() {
        return this.name;
    }

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    public boolean isBuiltin() {
        return false;
    }

    public boolean isTypedef() {
        return false;
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

    public boolean isEnum() {
        return false;
    }

    public ThriftType getTrueType() {
        return this;
    }

    public abstract <T> T accept(Visitor<? extends T> visitor);

    public abstract ThriftType withAnnotations(Map<String, String> annotations);

    public ThriftType withNamespaces(Map<NamespaceScope, String> namespaces) {
        return this;
    }

    public String getNamespace(NamespaceScope scope) {
        return "";
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

    public static final class BuiltinType extends ThriftType {
        BuiltinType(String name) {
            super(name);
        }

        BuiltinType(String name, Map<String, String> annotations) {
            super(name, annotations);
        }

        @Override
        public boolean isBuiltin() {
            return true;
        }

        @Override
        public <T> T accept(Visitor<? extends T> visitor) {
            if (this.equals(BOOL)) {
                return visitor.visitBool();
            } else if (this.equals(BYTE) || equals(I8)) {
                return visitor.visitByte();
            } else if (this.equals(I16)) {
                return visitor.visitI16();
            } else if (this.equals(I32)) {
                return visitor.visitI32();
            } else if (this.equals(I64)) {
                return visitor.visitI64();
            } else if (this.equals(DOUBLE)) {
                return visitor.visitDouble();
            } else if (this.equals(STRING)) {
                return visitor.visitString();
            } else if (this.equals(BINARY)) {
                return visitor.visitBinary();
            } else if (this.equals(VOID)) {
                return visitor.visitVoid();
            } else {
                throw new AssertionError("Unexpected built-in type: " + name());
            }
        }

        @Override
        public ThriftType withAnnotations(Map<String, String> annotations) {
            return new BuiltinType(this.name(), merge(this.annotations(), annotations));
        }
    }

    public static final class UserType extends ThriftType {
        private final Map<NamespaceScope, String> namespaces;
        private final boolean isEnum;

        UserType(
                String name,
                Map<NamespaceScope, String> namespaces,
                boolean isEnum,
                Map<String, String> annotations) {
            super(name, annotations);
            this.namespaces = namespaces;
            this.isEnum = isEnum;
        }

        @Override
        public String getNamespace(NamespaceScope scope) {
            String ns = namespaces.get(scope);
            if (ns == null && scope != NamespaceScope.ALL) {
                ns = namespaces.get(NamespaceScope.ALL);
            }
            return ns == null ? "" : ns;
        }

        @Override
        public boolean isEnum() {
            return isEnum;
        }

        @Override
        public <T> T accept(Visitor<? extends T> visitor) {
            if (isEnum) {
                return visitor.visitEnum(this);
            } else {
                return visitor.visitUserType(this);
            }
        }

        @Override
        public ThriftType withAnnotations(Map<String, String> annotations) {
            return new UserType(name(), namespaces, isEnum, merge(this.annotations(), annotations));
        }

        @Override
        public ThriftType withNamespaces(Map<NamespaceScope, String> newNamespaces) {
            return new UserType(name(), merge(namespaces, newNamespaces), isEnum, annotations());
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o) && isEnum == ((UserType) o).isEnum;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(super.hashCode(), isEnum);
        }
    }

    public static class ListType extends ThriftType {
        private final ThriftType elementType;

        ListType(String name, ThriftType elementType, Map<String, String> annotations) {
            super(name, annotations);
            this.elementType = elementType;
        }

        public ThriftType elementType() {
            return elementType;
        }

        @Override
        public <T> T accept(Visitor<? extends T> visitor) {
            return visitor.visitList(this);
        }

        @Override
        public ThriftType withAnnotations(Map<String, String> annotations) {
            return new ListType(name(), elementType, merge(this.annotations(), annotations));
        }

        public ListType withElementType(ThriftType newElementType) {
            return new ListType(name(), newElementType, annotations());
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

    public static final class SetType extends ThriftType {
        private final ThriftType elementType;

        SetType(String name, ThriftType elementType, Map<String, String> annotations) {
            super(name, annotations);
            this.elementType = elementType;
        }

        public ThriftType elementType() {
            return elementType;
        }

        @Override
        public <T> T accept(Visitor<? extends T> visitor) {
            return visitor.visitSet(this);
        }

        @Override
        public ThriftType withAnnotations(Map<String, String> annotations) {
            return new SetType(name(), elementType, merge(this.annotations(), annotations));
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

    public static final class MapType extends ThriftType {
        private final ThriftType keyType;
        private final ThriftType valueType;

        MapType(String name, ThriftType keyType, ThriftType valueType, Map<String, String> annotations) {
            super(name, annotations);
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
        public <T> T accept(Visitor<? extends T> visitor) {
            return visitor.visitMap(this);
        }

        @Override
        public ThriftType withAnnotations(Map<String, String> annotations) {
            return new MapType(name(), keyType, valueType, merge(this.annotations(), annotations));
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

    public static final class TypedefType extends ThriftType {
        private final ThriftType originalType;
        private final Map<NamespaceScope, String> namespaces;

        TypedefType(String name,
                ThriftType originalType,
                Map<NamespaceScope, String> namespaces,
                Map<String, String> annotations) {
            super(name, annotations);
            this.originalType = originalType;
            this.namespaces = namespaces;
        }

        @Override
        public boolean isTypedef() {
            return true;
        }

        public ThriftType originalType() {
            return originalType;
        }

        @Override
        public String getNamespace(NamespaceScope scope) {
            String ns = namespaces.get(scope);
            if (ns == null && scope != NamespaceScope.ALL) {
                ns = namespaces.get(NamespaceScope.ALL);
            }
            return ns == null ? "" : ns;
        }

        @Override
        public ThriftType getTrueType() {
            ThriftType t = originalType();
            while (t instanceof TypedefType) {
                t = ((TypedefType) t).originalType();
            }
            return t;
        }

        @Override
        public <T> T accept(Visitor<? extends T> visitor) {
            return visitor.visitTypedef(this);
        }

        @Override
        public ThriftType withAnnotations(Map<String, String> annotations) {
            return new TypedefType(name(), originalType, namespaces, merge(this.annotations(), annotations));
        }

        @Override
        public ThriftType withNamespaces(Map<NamespaceScope, String> newNamespaces) {
            return new TypedefType(name(), originalType, merge(namespaces, newNamespaces), annotations());
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }
            TypedefType that = (TypedefType) o;

            return originalType.equals(that.originalType);

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + originalType.hashCode();
            return result;
        }
    }

    public interface Visitor<T> {
        T visitBool();
        T visitByte();
        T visitI16();
        T visitI32();
        T visitI64();
        T visitDouble();
        T visitString();
        T visitBinary();
        T visitVoid();
        T visitEnum(ThriftType userType);
        T visitList(ListType listType);
        T visitSet(SetType setType);
        T visitMap(MapType mapType);
        T visitUserType(ThriftType userType);
        T visitTypedef(TypedefType typedefType);
    }
}
