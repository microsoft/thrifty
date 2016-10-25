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

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ThriftType {
    private final String name;

    ThriftType(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract <T> T accept(Visitor<T> visitor);

    public boolean isBuiltin() {
        return false;
    }

<<<<<<< 2ea12b53c3f56b9789f0441f59b0edebf625e3d7
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
=======
    public boolean isList() {
        return false;
    }

    public boolean isSet() {
        return false;
>>>>>>> Totally broken, but... Builders!  Types!  Change!
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

    public boolean isTypedef() {
        return false;
    }

    public boolean isService() {
        return false;
    }

    public ThriftType getTrueType() {
        return this;
    }

    abstract ThriftType withAnnotations(Map<String, String> annotations);

    public abstract ImmutableMap<String, String> annotations();

    protected static ImmutableMap<String, String> merge(
            ImmutableMap<String, String> baseAnnotations,
            Map<String, String> newAnnotations) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();
        merged.putAll(baseAnnotations);
        merged.putAll(newAnnotations);
        return ImmutableMap.copyOf(merged);
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

<<<<<<< 2ea12b53c3f56b9789f0441f59b0edebf625e3d7
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
=======
    public interface Visitor<T> {
        T visitBool(BuiltinThriftType boolType);
        T visitByte(BuiltinThriftType byteType);
        T visitI16(BuiltinThriftType i16Type);
        T visitI32(BuiltinThriftType i32Type);
        T visitI64(BuiltinThriftType i64Type);
        T visitDouble(BuiltinThriftType doubleType);
        T visitString(BuiltinThriftType stringType);
        T visitBinary(BuiltinThriftType binaryType);
>>>>>>> Totally broken, but... Builders!  Types!  Change!

        T visitEnum(EnumType enumType);

<<<<<<< 2ea12b53c3f56b9789f0441f59b0edebf625e3d7
        @Override
        public ThriftType withAnnotations(Map<String, String> annotations) {
            return new TypedefType(name(), originalType, namespaces, merge(this.annotations(), annotations));
        }
=======
        T visitList(ListType listType);
>>>>>>> Totally broken, but... Builders!  Types!  Change!

        T visitSet(SetType setType);

        T visitMap(MapType mapType);

        T visitStruct(StructType structType);

        T visitTypedef(TypedefType typedefType);

        T visitService(ServiceType serviceType);
    }
}
