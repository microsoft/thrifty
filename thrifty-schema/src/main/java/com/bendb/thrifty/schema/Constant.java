package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.ConstElement;
import com.bendb.thrifty.schema.parser.ConstValueElement;
import com.google.common.annotations.VisibleForTesting;

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

    void link(Linker linker) {
        this.type = linker.resolveType(element.type());
    }

    void validate(Linker linker) {
        if (type == null) {
            throw new IllegalStateException("Constants must be linked before validation");
        }

        ConstValueElement value = this.element.value();
        ThriftType type = this.type;

        validate(linker, value, type);
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
            if (value.kind() != ConstValueElement.Kind.IDENTIFIER) {
                throw new IllegalStateException("Expected 'true', 'false', or a bool constant, but got: "
                        + value.value());
            }

            String identifier = (String) value.value();
            if (!"true".equals(identifier) && !"false".equals(identifier)) {
                Named named = linker.lookupSymbol(identifier);
                if (named == null || named.type().getTrueType() != ThriftType.BOOL) {
                    throw new IllegalStateException("Expected 'true', 'false', or a bool constant, but got: "
                            + identifier);
                }
            }
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
            Named named = linker.lookupSymbol(expected.name());
            if (named instanceof EnumType) {
                EnumType et = (EnumType) named;

                if (value.kind() != ConstValueElement.Kind.IDENTIFIER) {
                    throw new IllegalStateException("bad enum literal");
                }

                String id = (String) value.value();
                for (EnumType.Member member : et.members()) {
                    if (member.name().equals(id)) {
                        return;
                    }
                }

                throw new IllegalStateException("");
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
