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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.ConstElement;
import com.microsoft.thrifty.schema.parser.ConstValueElement;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Represents a Thrift const definition.
 */
public class Constant implements UserElement {
    private final ConstElement element;
    private final ImmutableMap<NamespaceScope, String> namespaces;
    private final UserElementMixin mixin;

    private ThriftType type;

    Constant(ConstElement element, ImmutableMap<NamespaceScope, String> namespaces) {
        this.element = element;
        this.namespaces = namespaces;
        this.mixin = new UserElementMixin(
                element.name(),
                element.location(),
                element.documentation(),
                null); // No annotations allowed on Thrift constants
    }

    public ThriftType type() {
        return type;
    }

    public ConstValueElement value() {
        return element.value();
    }

    @Nullable
    public String getNamespaceFor(NamespaceScope scope) {
        String ns = namespaces.get(scope);
        if (ns == null && scope != NamespaceScope.ALL) {
            ns = namespaces.get(NamespaceScope.ALL);
        }
        return ns;
    }

    @Override
    public String name() {
        return mixin.name();
    }

    @Override
    public Location location() {
        return mixin.location();
    }

    @Override
    public String documentation() {
        return mixin.documentation();
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return ImmutableMap.of();
    }

    @Override
    public boolean hasJavadoc() {
        return mixin.hasJavadoc();
    }

    @Override
    public boolean isDeprecated() {
        return mixin.isDeprecated();
    }

    void link(Linker linker) {
        type = linker.resolveType(element.type());
    }

    void validate(Linker linker) {
        validate(linker, element.value(), type);
    }

    @VisibleForTesting
    static void validate(Linker linker, ConstValueElement value, ThriftType expected) {
        ThriftType trueType = expected.getTrueType();
        Validators.forType(trueType).validate(linker, trueType, value);
    }

    interface ConstValueValidator {
        void validate(Linker linker, ThriftType expected, ConstValueElement value);
    }

    private static class Validators {
        private static final ConstValueValidator BOOL   = new BoolValidator();
        private static final ConstValueValidator BYTE   = new IntegerValidator(Byte.MIN_VALUE, Byte.MAX_VALUE);
        private static final ConstValueValidator I16    = new IntegerValidator(Short.MIN_VALUE, Short.MAX_VALUE);
        private static final ConstValueValidator I32    = new IntegerValidator(Integer.MIN_VALUE, Integer.MAX_VALUE);
        private static final ConstValueValidator I64    = new IntegerValidator(Long.MIN_VALUE, Long.MAX_VALUE);
        private static final ConstValueValidator DOUBLE = new BaseValidator(ConstValueElement.Kind.DOUBLE);
        private static final ConstValueValidator STRING = new BaseValidator(ConstValueElement.Kind.STRING);

        private static final ConstValueValidator ENUM = new EnumValidator();
        private static final ConstValueValidator COLLECTION = new CollectionValidator();
        private static final ConstValueValidator MAP = new MapValidator();

        static ConstValueValidator forType(ThriftType type) {
            if (type.isTypedef()) {
                type = type.getTrueType();
            }

            if (type.isBuiltin()) {
                if (type.equals(BuiltinType.BOOL))   return BOOL;
                if (type.equals(BuiltinType.BYTE))   return BYTE;
                if (type.equals(BuiltinType.I16))    return I16;
                if (type.equals(BuiltinType.I32))    return I32;
                if (type.equals(BuiltinType.I64))    return I64;
                if (type.equals(BuiltinType.DOUBLE)) return DOUBLE;
                if (type.equals(BuiltinType.STRING)) return STRING;

                if (type.equals(BuiltinType.BINARY)) {
                    throw new IllegalStateException("Binary constants are unsupported");
                }

                if (type.equals(BuiltinType.VOID)) {
                    throw new IllegalStateException("Cannot declare a constant of type 'void'");
                }

                throw new AssertionError("Unrecognized built-in type: " + type.name());
            }

            if (type.isEnum()) {
                return ENUM;
            }

            if (type.isList() || type.isSet()) {
                return COLLECTION;
            }

            if (type.isMap()) {
                return MAP;
            }

            throw new IllegalStateException("Struct-valued constants are not yet implemented");
        }
    }

    private static class BoolValidator implements ConstValueValidator {
        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (value.kind() == ConstValueElement.Kind.INTEGER) {
                int n = value.getAsInt();
                if (n == 0 || n == 1) {
                    return;
                }
            } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                String identifier = (String) value.value();
                if ("true".equals(identifier) || "false".equals(identifier)) {
                    return;
                }

                Constant constant = linker.lookupConst(identifier);
                if (constant != null && constant.type().getTrueType().equals(BuiltinType.BOOL)) {
                    return;
                }
            }

            throw new IllegalStateException(
                    "Expected 'true', 'false', '1', '0', or a bool constant; got: "
                            + value.value() + " at " + value.location());
        }
    }

    private static class BaseValidator implements ConstValueValidator {
        private final ConstValueElement.Kind expectedKind;

        BaseValidator(ConstValueElement.Kind expectedKind) {
            this.expectedKind = expectedKind;
        }

        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (value.kind() == expectedKind) {
                return;
            }

            if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                String id = (String) value.value();
                Constant constant = linker.lookupConst(id);

                if (constant == null) {
                    throw new IllegalStateException("Unrecognized const identifier: " + id);
                }

                if (!constant.type().getTrueType().equals(expected)) {
                    throw new IllegalStateException("Expected a value of type " + expected.name()
                            + ", but got " + constant.type().name());
                }
            } else {
                throw new IllegalStateException(
                        "Expected a value of type " + expected.name().toLowerCase()
                                + " but got " + value.value());
            }
        }
    }

    private static class IntegerValidator extends BaseValidator {
        private final long minValue;

        private final long maxValue;

        IntegerValidator(long minValue, long maxValue) {
            super(ConstValueElement.Kind.INTEGER);
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            super.validate(linker, expected, value);

            if (value.kind() == ConstValueElement.Kind.INTEGER) {
                Long lv = (Long) value.value();
                if (lv < minValue || lv > maxValue) {
                    throw new IllegalStateException("value '" + String.valueOf(lv)
                            + "' is out of range for type " + expected.name());
                }
            }
        }
    }

    private static class EnumValidator implements ConstValueValidator {
        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (!expected.isEnum()) {
                throw new IllegalStateException("bad enum literal");
            }

            EnumType et = (EnumType) expected;

            if (value.kind() == ConstValueElement.Kind.INTEGER) {
                long id = (Long) value.value();
                for (EnumMember member : et.members()) {
                    if (member.value() == id) {
                        return;
                    }
                }
                throw new IllegalStateException("'" + id + "' is not a valid value for " + et.name());
            } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                // An IDENTIFIER enum value could be one of four kinds of entities:
                // 1. Another constant, possibly of the correct type
                // 2. A fully-qualified imported enum value, e.g. file.Enum.Member
                // 3. An imported, partially-qualified enum value, e.g. Enum.Member (where Enum is imported)
                // 4. A fully-qualified, non-imported enum value, e.g. Enum.Member
                //
                // Apache accepts all of these, and so do we.

                String id = (String) value.value();

                // An unusual edge case is when a named constant has the same name as an enum
                // member; in this case, constants take precedence over members.  Make sure that
                // the type is as expected!
                Constant constant = linker.lookupConst(id);
                if (constant != null && constant.type().getTrueType().equals(expected)) {
                    return;
                }

                int ix = id.lastIndexOf('.');
                if (ix == -1) {
                    throw new IllegalStateException(
                            "Unqualified name '" + id + "' is not a valid enum constant value: ("
                                    + value.location() + ")");
                }

                String typeName = id.substring(0, ix); // possibly qualified
                String memberName = id.substring(ix + 1);

                // Does the literal name match the expected type name?
                // It could be that typeName is qualified; handle that case.
                boolean typeNameMatches = false;
                ix = typeName.indexOf('.');
                if (ix == -1) {
                    // unqualified
                    if (expected.name().equals(typeName)) {
                        typeNameMatches = true;
                    }
                } else {
                    // qualified
                    String qualifier = typeName.substring(0, ix);
                    String actualName = typeName.substring(ix + 1);

                    // Does the qualifier match?
                    if (et.location().getProgramName().equals(qualifier) && et.name().equals(actualName)) {
                        typeNameMatches = true;
                    }
                }

                if (typeNameMatches) {
                    for (EnumMember member : et.members()) {
                        if (member.name().equals(memberName)) {
                            return;
                        }
                    }
                }

                throw new IllegalStateException(
                        "'" + id + "' is not a member of enum type " + et.name() + ": members=" + et.members());
            } else {
                throw new IllegalStateException("bad enum literal: " + value.value());
            }
        }
    }

    private static class CollectionValidator implements ConstValueValidator {
        @SuppressWarnings("unchecked")
        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (value.kind() == ConstValueElement.Kind.LIST) {
                List<ConstValueElement> list = value.getAsList();

                ThriftType elementType;
                if (expected.isList()) {
                    elementType = ((ListType) expected).elementType().getTrueType();
                } else if (expected.isSet()) {
                    elementType = ((SetType) expected).elementType().getTrueType();
                } else {
                    throw new AssertionError();
                }

                for (ConstValueElement element : list) {
                    Constant.validate(linker, element, elementType);
                }
            } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                String id = (String) value.value();
                Constant named = linker.lookupConst(id);

                boolean isConstantOfCorrectType = named != null
                        && named.type().getTrueType().equals(expected);

                if (!isConstantOfCorrectType) {
                    throw new IllegalStateException("Expected a value with type " + expected.name());
                }
            } else {
                throw new IllegalStateException("Expected a list literal, got: " + value.value());
            }
        }
    }

    private static class MapValidator implements ConstValueValidator {
        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (value.kind() == ConstValueElement.Kind.MAP) {
                Map<ConstValueElement, ConstValueElement> map = value.getAsMap();

                MapType mapType = (MapType) expected;
                ThriftType keyType = mapType.keyType().getTrueType();
                ThriftType valueType = mapType.valueType().getTrueType();

                for (Map.Entry<ConstValueElement, ConstValueElement> entry : map.entrySet()) {
                    Constant.validate(linker, entry.getKey(), keyType);
                    Constant.validate(linker, entry.getValue(), valueType);
                }
            } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                String id = (String) value.value();
                Constant named = linker.lookupConst(id);

                boolean isConstantOfCorrectType = named != null
                        && named.type().getTrueType().equals(expected);

                if (!isConstantOfCorrectType) {
                    throw new IllegalStateException("Expected a value with type " + expected.name());
                }
            } else {
                throw new IllegalStateException("Expected a map literal, got: " + value.value());
            }
        }
    }
}
