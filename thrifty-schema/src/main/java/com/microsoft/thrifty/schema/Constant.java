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
import com.microsoft.thrifty.schema.parser.ConstElement;
import com.microsoft.thrifty.schema.parser.ConstValueElement;

import java.util.List;
import java.util.Map;

public class Constant extends Named {
    private final ConstElement element;
    private ThriftType type;

    Constant(ConstElement element, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
    }

    @Override
    public ThriftType type() {
        return type;
    }

    public String documentation() {
        return element.documentation();
    }

    @Override
    public Location location() {
        return element.location();
    }

    public ConstValueElement value() {
        return element.value();
    }

    void link(Linker linker) {
        this.type = linker.resolveType(element.type());
    }

    void validate(Linker linker) {
        if (type == null) {
            throw new IllegalStateException("Constants must be linked before validation");
        }

        ConstValueElement value = this.element.value();
        ThriftType type = this.type;

        try {
            validate(linker, value, type);
        } catch (IllegalStateException e) {
            linker.addError(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @VisibleForTesting
    static void validate(Linker linker, ConstValueElement value, ThriftType expectedType) {
        Validators.forType(expectedType).validate(linker, expectedType, value);
    }

    interface ConstValueValidator {
        void validate(Linker linker, ThriftType expected, ConstValueElement value);
    }

    static class Validators {
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
                if (type == ThriftType.BOOL)   return BOOL;
                if (type == ThriftType.BYTE)   return BYTE;
                if (type == ThriftType.I16)    return I16;
                if (type == ThriftType.I32)    return I32;
                if (type == ThriftType.I64)    return I64;
                if (type == ThriftType.DOUBLE) return DOUBLE;
                if (type == ThriftType.STRING) return STRING;

                if (type == ThriftType.BINARY) {
                    throw new IllegalStateException("Binary constants are unsupported");
                }

                if (type == ThriftType.VOID) {
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

    static class BoolValidator implements ConstValueValidator {
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

                Named named = linker.lookupSymbol(identifier);
                if (named != null && named.type().getTrueType() == ThriftType.BOOL) {
                    return;
                }
            }

            throw new IllegalStateException(
                    "Expected 'true', 'false', '1', '0', or a bool constant; got: "
                            + value.value() + " at " + value.location());
        }
    }

    static class BaseValidator implements ConstValueValidator {
        private final ConstValueElement.Kind expectedKind;

        protected BaseValidator(ConstValueElement.Kind expectedKind) {
            this.expectedKind = expectedKind;
        }

        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (value.kind() == expectedKind) {
                return;
            }

            if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                String id = (String) value.value();
                Named named = linker.lookupSymbol(id);

                if (named == null) {
                    throw new IllegalStateException("Unrecognized identifier: " + id);
                }

                if (!named.type().getTrueType().equals(expected)) {
                    throw new IllegalStateException("Expected a value of type " + expected.name()
                            + ", but got " + named.type().name());
                }
            } else {
                throw new IllegalStateException(
                        "Expected a value of type " + expected.name().toLowerCase()
                                + " but got " + value.value());
            }
        }
    }

    static class IntegerValidator extends BaseValidator {
        private final long minValue;

        private final long maxValue;

        protected IntegerValidator(long minValue, long maxValue) {
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

    static class EnumValidator implements ConstValueValidator {
        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            Named named = linker.lookupSymbol(expected);
            if (named instanceof EnumType) {
                EnumType et = (EnumType) named;

                if (value.kind() == ConstValueElement.Kind.INTEGER) {
                    long id = (Long) value.value();
                    for (EnumType.Member member : et.members()) {
                        if (member.value().longValue() == id) {
                            return;
                        }
                    }
                    throw new IllegalStateException("'" + id + "' is not a valid value for " + et.name());
                } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                    String id = (String) value.value();

                    int ix = id.lastIndexOf('.');
                    if (ix != -1) {
                        String typeName = id.substring(0, ix);
                        String memberName = id.substring(ix + 1);

                        Named namedType = linker.lookupSymbol(typeName);
                        if (namedType != null && namedType.type().equals(expected)) {
                            for (EnumType.Member member : et.members()) {
                                if (member.name().equals(memberName)) {
                                    return;
                                }
                            }
                        }
                    }

                    throw new IllegalStateException(
                            "'" + id + "' is not a member of enum type " + et.name() + ": members=" + et.members());
                } else {
                    throw new IllegalStateException("bad enum literal: " + value.value());
                }
            } else if (named instanceof Constant) {
                if (!named.type().getTrueType().equals(expected)) {
                    throw new IllegalStateException("Invalid type");
                }
            } else {
                throw new IllegalStateException("bad enum literal");
            }
        }
    }

    static class CollectionValidator implements ConstValueValidator {
        @SuppressWarnings("unchecked")
        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (value.kind() == ConstValueElement.Kind.LIST) {
                List<ConstValueElement> list = (List<ConstValueElement>) value.value();

                ThriftType elementType;
                if (expected.isList()) {
                    elementType = ((ThriftType.ListType) expected).elementType().getTrueType();
                } else if (expected.isSet()) {
                    elementType = ((ThriftType.SetType) expected).elementType().getTrueType();
                } else {
                    throw new AssertionError();
                }

                for (ConstValueElement element : list) {
                    Constant.validate(linker, element, elementType);
                }
            } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                String id = (String) value.value();
                Named named = linker.lookupSymbol(id);

                boolean isConstantOfCorrectType =
                        named instanceof Constant
                        && named.type().getTrueType().equals(expected);

                if (!isConstantOfCorrectType) {
                    throw new IllegalStateException("Expected a value with type " + expected.name());
                }
            } else {
                throw new IllegalStateException("Expected a list literal, got: " + value.value());
            }
        }
    }

    static class MapValidator implements ConstValueValidator {
        @SuppressWarnings("unchecked")
        @Override
        public void validate(Linker linker, ThriftType expected, ConstValueElement value) {
            if (value.kind() == ConstValueElement.Kind.MAP) {
                Map<ConstValueElement, ConstValueElement> map =
                        (Map<ConstValueElement, ConstValueElement>) value.value();

                ThriftType.MapType mapType = (ThriftType.MapType) expected;
                ThriftType keyType = mapType.keyType().getTrueType();
                ThriftType valueType = mapType.valueType().getTrueType();

                for (Map.Entry<ConstValueElement, ConstValueElement> entry : map.entrySet()) {
                    Constant.validate(linker, entry.getKey(), keyType);
                    Constant.validate(linker, entry.getValue(), valueType);
                }
            } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                String id = (String) value.value();
                Named named = linker.lookupSymbol(id);

                boolean isConstantOfCorrectType =
                        named instanceof Constant
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
