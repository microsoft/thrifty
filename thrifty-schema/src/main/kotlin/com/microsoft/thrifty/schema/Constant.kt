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
package com.microsoft.thrifty.schema

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableMap
import com.microsoft.thrifty.schema.parser.ConstElement
import com.microsoft.thrifty.schema.parser.ConstValueElement
import java.util.UUID

/**
 * Represents a Thrift const definition.
 */
class Constant : UserElement {
    private val element: ConstElement
    private val namespaces: Map<NamespaceScope, String>?
    private val mixin: UserElementMixin

    private var type: ThriftType? = null

    override val isDeprecated: Boolean
        get() = mixin.isDeprecated

    internal constructor(element: ConstElement, namespaces: Map<NamespaceScope, String>) {
        this.element = element
        this.namespaces = namespaces
        this.mixin = UserElementMixin(
                element.uuid,
                element.name,
                element.location,
                element.documentation, null) // No annotations allowed on Thrift constants
    }

    protected constructor(builder: Builder) {
        this.element = builder.element
        this.namespaces = builder.namespaces
        this.mixin = builder.mixin
        this.type = builder.type
    }

    fun type(): ThriftType {
        return type!!
    }

    fun value(): ConstValueElement {
        return element.value
    }

    fun getNamespaceFor(scope: NamespaceScope): String? {
        var ns: String? = namespaces!![scope]
        if (ns == null && scope !== NamespaceScope.ALL) {
            ns = namespaces[NamespaceScope.ALL]
        }
        return ns
    }

    override fun uuid(): UUID {
        return mixin.uuid()
    }

    override fun name(): String {
        return mixin.name()
    }

    override fun location(): Location {
        return mixin.location()
    }

    override fun documentation(): String {
        return mixin.documentation()
    }

    override fun annotations(): ImmutableMap<String, String> {
        return ImmutableMap.of()
    }

    override fun hasJavadoc(): Boolean {
        return mixin.hasJavadoc()
    }

    fun namespaces(): Map<NamespaceScope, String>? {
        return namespaces
    }

    internal fun link(linker: Linker) {
        type = linker.resolveType(element.type)
    }

    internal fun validate(linker: Linker) {
        validate(linker, element.value, type!!)
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder internal constructor(constant: Constant) : AbstractUserElementBuilder<Constant, Builder>(constant.mixin) {

        internal val element: ConstElement = constant.element
        internal var namespaces: Map<NamespaceScope, String>? = constant.namespaces
        internal val type: ThriftType? = constant.type

        fun namespaces(namespaces: Map<NamespaceScope, String>): Builder {
            this.namespaces = namespaces
            return this
        }

        override fun build(): Constant {
            return Constant(this)
        }
    }

    internal interface ConstValueValidator {
        fun validate(symbolTable: SymbolTable, expected: ThriftType, value: ConstValueElement)
    }

    private object Validators {
        private val BOOL = BoolValidator()
        private val BYTE = IntegerValidator(java.lang.Byte.MIN_VALUE.toLong(), java.lang.Byte.MAX_VALUE.toLong())
        private val I16 = IntegerValidator(java.lang.Short.MIN_VALUE.toLong(), java.lang.Short.MAX_VALUE.toLong())
        private val I32 = IntegerValidator(Integer.MIN_VALUE.toLong(), Integer.MAX_VALUE.toLong())
        private val I64 = IntegerValidator(java.lang.Long.MIN_VALUE, java.lang.Long.MAX_VALUE)
        private val DOUBLE = BaseValidator(ConstValueElement.Kind.DOUBLE)
        private val STRING = BaseValidator(ConstValueElement.Kind.STRING)

        private val ENUM = EnumValidator()
        private val COLLECTION = CollectionValidator()
        private val MAP = MapValidator()

        internal fun forType(type: ThriftType): ConstValueValidator {
            val tt = type.trueType

            if (tt.isBuiltin) {
                if (tt == BuiltinType.BOOL) return BOOL
                if (tt == BuiltinType.BYTE) return BYTE
                if (tt == BuiltinType.I16) return I16
                if (tt == BuiltinType.I32) return I32
                if (tt == BuiltinType.I64) return I64
                if (tt == BuiltinType.DOUBLE) return DOUBLE
                if (type == BuiltinType.STRING) return STRING

                if (tt == BuiltinType.BINARY) {
                    throw IllegalStateException("Binary constants are unsupported")
                }

                if (tt == BuiltinType.VOID) {
                    throw IllegalStateException("Cannot declare a constant of type 'void'")
                }

                throw AssertionError("Unrecognized built-in type: " + type.name())
            }

            if (tt.isEnum) {
                return ENUM
            }

            if (tt.isList || tt.isSet) {
                return COLLECTION
            }

            if (tt.isMap) {
                return MAP
            }

            throw IllegalStateException("Struct-valued constants are not yet implemented")
        }
    }

    private class BoolValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, value: ConstValueElement) {
            if (value.kind === ConstValueElement.Kind.INTEGER) {
                val n = value.getAsInt()
                if (n == 0 || n == 1) {
                    return
                }
            } else if (value.kind === ConstValueElement.Kind.IDENTIFIER) {
                val identifier = value.value as String
                if ("true" == identifier || "false" == identifier) {
                    return
                }

                val constant = symbolTable.lookupConst(identifier)
                if (constant != null && constant.type().trueType == BuiltinType.BOOL) {
                    return
                }
            }

            throw IllegalStateException(
                    "Expected 'true', 'false', '1', '0', or a bool constant; got: "
                            + value.value + " at " + value.location)
        }
    }

    private open class BaseValidator internal constructor(private val expectedKind: ConstValueElement.Kind) : ConstValueValidator {

        override fun validate(symbolTable: SymbolTable, expected: ThriftType, value: ConstValueElement) {
            if (value.kind === expectedKind) {
                return
            }

            if (value.kind === ConstValueElement.Kind.IDENTIFIER) {
                val id = value.value as String
                val constant = symbolTable.lookupConst(id)
                        ?: throw IllegalStateException("Unrecognized const identifier: $id")

                if (constant.type().trueType != expected) {
                    throw IllegalStateException("Expected a value of type " + expected.name()
                            + ", but got " + constant.type().name())
                }
            } else {
                throw IllegalStateException(
                        "Expected a value of type " + expected.name().toLowerCase()
                                + " but got " + value.value)
            }
        }
    }

    private class IntegerValidator internal constructor(private val minValue: Long, private val maxValue: Long) : BaseValidator(ConstValueElement.Kind.INTEGER) {

        override fun validate(symbolTable: SymbolTable, expected: ThriftType, value: ConstValueElement) {
            super.validate(symbolTable, expected, value)

            if (value.kind === ConstValueElement.Kind.INTEGER) {
                val lv = value.value as Long
                if (lv < minValue || lv > maxValue) {
                    throw IllegalStateException("value '" + lv.toString()
                            + "' is out of range for type " + expected.name())
                }
            }
        }
    }

    private class EnumValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, value: ConstValueElement) {
            if (!expected.isEnum) {
                throw IllegalStateException("bad enum literal")
            }

            val et = expected as EnumType

            if (value.kind === ConstValueElement.Kind.INTEGER) {
                val id = value.getAsLong()
                for (member in et.members) {
                    if (member.value.toLong() == id) {
                        return
                    }
                }
                throw IllegalStateException("'" + id + "' is not a valid value for " + et.name())
            } else if (value.kind === ConstValueElement.Kind.IDENTIFIER) {
                // An IDENTIFIER enum value could be one of four kinds of entities:
                // 1. Another constant, possibly of the correct type
                // 2. A fully-qualified imported enum value, e.g. file.Enum.Member
                // 3. An imported, partially-qualified enum value, e.g. Enum.Member (where Enum is imported)
                // 4. A fully-qualified, non-imported enum value, e.g. Enum.Member
                //
                // Apache accepts all of these, and so do we.

                val id = value.value as String

                // An unusual edge case is when a named constant has the same name as an enum
                // member; in this case, constants take precedence over members.  Make sure that
                // the type is as expected!
                val constant = symbolTable.lookupConst(id)
                if (constant != null && constant.type().trueType == expected) {
                    return
                }

                var ix = id.lastIndexOf('.')
                if (ix == -1) {
                    throw IllegalStateException(
                            "Unqualified name '" + id + "' is not a valid enum constant value: ("
                                    + value.location + ")")
                }

                val typeName = id.substring(0, ix) // possibly qualified
                val memberName = id.substring(ix + 1)

                // Does the literal name match the expected type name?
                // It could be that typeName is qualified; handle that case.
                var typeNameMatches = false
                ix = typeName.indexOf('.')
                if (ix == -1) {
                    // unqualified
                    if (expected.name() == typeName) {
                        typeNameMatches = true
                    }
                } else {
                    // qualified
                    val qualifier = typeName.substring(0, ix)
                    val actualName = typeName.substring(ix + 1)

                    // Does the qualifier match?
                    if (et.location().programName == qualifier && et.name() == actualName) {
                        typeNameMatches = true
                    }
                }

                if (typeNameMatches) {
                    for (member in et.members) {
                        if (member.name() == memberName) {
                            return
                        }
                    }
                }

                throw IllegalStateException(
                        "'" + id + "' is not a member of enum type " + et.name() + ": members=" + et.members)
            } else {
                throw IllegalStateException("bad enum literal: " + value.value)
            }
        }
    }

    private class CollectionValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, value: ConstValueElement) {
            if (value.kind === ConstValueElement.Kind.LIST) {
                val list = value.getAsList()

                val elementType: ThriftType
                if (expected.isList) {
                    elementType = (expected as ListType).elementType().trueType
                } else if (expected.isSet) {
                    elementType = (expected as SetType).elementType().trueType
                } else {
                    throw AssertionError()
                }

                for (element in list) {
                    Constant.validate(symbolTable, element, elementType)
                }
            } else if (value.kind === ConstValueElement.Kind.IDENTIFIER) {
                val id = value.value as String
                val named = symbolTable.lookupConst(id)

                val isConstantOfCorrectType = named != null && named.type().trueType == expected

                if (!isConstantOfCorrectType) {
                    throw IllegalStateException("Expected a value with type " + expected.name())
                }
            } else {
                throw IllegalStateException("Expected a list literal, got: " + value.value)
            }
        }
    }

    private class MapValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, value: ConstValueElement) {
            if (value.kind === ConstValueElement.Kind.MAP) {
                val map = value.getAsMap()

                val mapType = expected as MapType
                val keyType = mapType.keyType().trueType
                val valueType = mapType.valueType().trueType

                for ((key, value1) in map) {
                    Constant.validate(symbolTable, key, keyType)
                    Constant.validate(symbolTable, value1, valueType)
                }
            } else if (value.kind === ConstValueElement.Kind.IDENTIFIER) {
                val id = value.value as String
                val named = symbolTable.lookupConst(id)

                val isConstantOfCorrectType = named != null && named.type().trueType == expected

                if (!isConstantOfCorrectType) {
                    throw IllegalStateException("Expected a value with type " + expected.name())
                }
            } else {
                throw IllegalStateException("Expected a map literal, got: " + value.value)
            }
        }
    }

    companion object {

        @VisibleForTesting
        @JvmStatic
        internal fun validate(symbolTable: SymbolTable, value: ConstValueElement, expected: ThriftType) {
            val trueType = expected.trueType
            Validators.forType(trueType).validate(symbolTable, trueType, value)
        }
    }
}
