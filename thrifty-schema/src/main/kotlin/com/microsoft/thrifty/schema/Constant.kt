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
import com.microsoft.thrifty.schema.parser.ConstElement
import com.microsoft.thrifty.schema.parser.ConstValueElement
import com.microsoft.thrifty.schema.parser.DoubleValueElement
import com.microsoft.thrifty.schema.parser.IdentifierValueElement
import com.microsoft.thrifty.schema.parser.IntValueElement
import com.microsoft.thrifty.schema.parser.ListValueElement
import com.microsoft.thrifty.schema.parser.LiteralValueElement
import com.microsoft.thrifty.schema.parser.MapValueElement

/**
 * Represents a Thrift const definition.
 */
class Constant private constructor (
        private val element: ConstElement,
        private val mixin: UserElementMixin,
        private var type_: ThriftType? = null
) : UserElement by mixin {

    /**
     * The type of the const value.
     */
    val type: ThriftType
        get() = type_!!

    /**
     * The const's value.
     */
    val value: ConstValueElement
        get() = element.value

    override val isDeprecated: Boolean
        get() = mixin.isDeprecated

    internal constructor(element: ConstElement, namespaces: Map<NamespaceScope, String>, type: ThriftType? = null)
            : this(element, UserElementMixin(element, namespaces), type)

    private constructor(builder: Builder)
            : this(builder.element, builder.mixin, builder.type)

    internal fun link(linker: Linker) {
        type_ = linker.resolveType(element.type)
    }

    internal fun validate(linker: Linker) {
        validate(linker, element.value, type)
    }

    /**
     * Returns a builder initialized with this constant's values.
     */
    fun toBuilder(): Builder {
        return Builder(this)
    }

    /**
     * An object that can build [Constants][Constant].
     */
    class Builder internal constructor(
            constant: Constant
    ) : AbstractUserElementBuilder<Constant, Builder>(constant.mixin) {

        internal val element: ConstElement = constant.element
        internal val type: ThriftType? = constant.type

        override fun build(): Constant {
            return Constant(this)
        }
    }

    internal interface ConstValueValidator {
        fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement)
    }

    private object Validators {
        private val BOOL = BoolValidator
        private val BYTE = IntegerValidator(java.lang.Byte.MIN_VALUE.toLong(), java.lang.Byte.MAX_VALUE.toLong())
        private val I16 = IntegerValidator(java.lang.Short.MIN_VALUE.toLong(), java.lang.Short.MAX_VALUE.toLong())
        private val I32 = IntegerValidator(Integer.MIN_VALUE.toLong(), Integer.MAX_VALUE.toLong())
        private val I64 = IntegerValidator(java.lang.Long.MIN_VALUE, java.lang.Long.MAX_VALUE)
        private val DOUBLE = DoubleValidator
        private val STRING = StringValidator

        private val ENUM = EnumValidator
        private val COLLECTION = CollectionValidator
        private val MAP = MapValidator

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

                throw AssertionError("Unrecognized built-in type: ${type.name}")
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

    private object BoolValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            when (valueElement) {
                is IntValueElement -> {
                    if (valueElement.value in listOf(0L, 1L)) {
                        return
                    }
                }

                is IdentifierValueElement -> {
                    val identifier = valueElement.value
                    if ("true" == identifier || "false" == identifier) {
                        return
                    }

                    val constant = symbolTable.lookupConst(identifier)
                    if (constant != null && constant.type.trueType == BuiltinType.BOOL) {
                        return
                    }
                }
            }

            throw IllegalStateException(
                    "Expected 'true', 'false', '1', '0', or a bool constant; got: $valueElement at ${valueElement.location}")
        }
    }

    private open class BaseValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            if (valueElement is IdentifierValueElement) {
                val id = valueElement.value
                val constant = symbolTable.lookupConst(id)
                        ?: throw IllegalStateException("Unrecognized const identifier: $id")

                if (constant.type.trueType != expected) {
                    throw IllegalStateException(
                            "Expected a value of type ${expected.name}, but got ${constant.type.name}")
                }
            } else {
                throw IllegalStateException(
                        "Expected a value of type ${expected.name.toLowerCase()} but got $valueElement")
            }
        }
    }

    private class IntegerValidator(
            private val minValue: Long,
            private val maxValue: Long
    ) : BaseValidator() {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            when (valueElement) {
                is IntValueElement -> {
                    val lv = valueElement.value
                    if (lv < minValue || lv > maxValue) {
                        throw IllegalStateException("value '$lv' is out of range for type ${expected.name}")
                    }
                }

                else -> super.validate(symbolTable, expected, valueElement)
            }
        }
    }

    private object DoubleValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            when (valueElement) {
                is IntValueElement -> return
                is DoubleValueElement -> return
                is IdentifierValueElement -> {
                    // maybe a const?
                    val id = valueElement.value
                    val constant = symbolTable.lookupConst(id)
                            ?: throw IllegalStateException("Unrecognized const identifier: $id")

                    if (constant.type.trueType != expected) {
                        throw IllegalStateException(
                                "Expected a value of type ${expected.name}, but got ${constant.type.name}")
                    }
                }

                else -> throw IllegalStateException("Expected a value of type DOUBLE but got $valueElement")
            }
        }
    }

    private object StringValidator : BaseValidator() {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            if (valueElement !is LiteralValueElement) {
                super.validate(symbolTable, expected, valueElement)
            }
        }
    }

    private object EnumValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            if (expected !is EnumType) {
                throw IllegalStateException("bad enum literal")
            }

            when (valueElement) {
                is IntValueElement -> {
                    val id = valueElement.value
                    if (expected.members.any { it.value.toLong() == id }) {
                        return
                    }
                    throw IllegalStateException("'$id' is not a valid value for ${expected.name}")
                }

                is IdentifierValueElement -> {
                    // An IDENTIFIER enum value could be one of four kinds of entities:
                    // 1. Another constant, possibly of the correct type
                    // 2. A fully-qualified imported enum value, e.g. file.Enum.Member
                    // 3. An imported, partially-qualified enum value, e.g. Enum.Member (where Enum is imported)
                    // 4. A fully-qualified, non-imported enum value, e.g. Enum.Member
                    //
                    // Apache accepts all of these, and so do we.
                    val id = valueElement.value

                    // An unusual edge case is when a named constant has the same name as an enum
                    // member; in this case, constants take precedence over members.  Make sure that
                    // the type is as expected!
                    val constant = symbolTable.lookupConst(id)
                    if (constant != null && constant.type.trueType == expected) {
                        return
                    }

                    var ix = id.lastIndexOf('.')
                    if (ix == -1) {
                        throw IllegalStateException(
                                "Unqualified name '$id' is not a valid enum constant value: (${valueElement.location})")
                    }

                    val typeName = id.substring(0, ix) // possibly qualified
                    val memberName = id.substring(ix + 1)

                    // Does the literal name match the expected type name?
                    // It could be that typeName is qualified; handle that case.
                    var typeNameMatches = false
                    ix = typeName.indexOf('.')
                    if (ix == -1) {
                        // unqualified
                        if (expected.name == typeName) {
                            typeNameMatches = true
                        }
                    } else {
                        // qualified
                        val qualifier = typeName.substring(0, ix)
                        val actualName = typeName.substring(ix + 1)

                        // Does the qualifier match?
                        if (expected.location.programName == qualifier && expected.name == actualName) {
                            typeNameMatches = true
                        }
                    }

                    if (typeNameMatches && expected.members.any { it.name == memberName }) {
                        return
                    }

                    throw IllegalStateException(
                            "'$id' is not a member of enum type ${expected.name}: members=${expected.members}")
                }

                else -> throw IllegalStateException("bad enum literal: $valueElement")
            }
        }
    }

    private object CollectionValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            when (valueElement) {
                is ListValueElement -> {
                    val list = valueElement.value
                    val elementType = when (expected) {
                        is ListType -> expected.elementType.trueType
                        is SetType -> expected.elementType.trueType
                        else -> throw AssertionError("Unexpectedly not a collection type: $expected")
                    }

                    for (element in list) {
                        Constant.validate(symbolTable, element, elementType)
                    }
                }

                is IdentifierValueElement -> {
                    val id = valueElement.value
                    val named = symbolTable.lookupConst(id)

                    val isConstantOfCorrectType = named != null && named.type.trueType == expected

                    if (!isConstantOfCorrectType) {
                        throw IllegalStateException("Expected a value with type ${expected.name}")
                    }
                }

                else -> throw IllegalStateException("Expected a list literal, got: $valueElement")
            }
        }
    }

    private object MapValidator : ConstValueValidator {
        override fun validate(symbolTable: SymbolTable, expected: ThriftType, valueElement: ConstValueElement) {
            when (valueElement) {
                is MapValueElement -> {
                    val map = valueElement.value

                    val mapType = expected as MapType
                    val keyType = mapType.keyType.trueType
                    val valueType = mapType.valueType.trueType

                    for ((key, value1) in map) {
                        Constant.validate(symbolTable, key, keyType)
                        Constant.validate(symbolTable, value1, valueType)
                    }
                }

                is IdentifierValueElement -> {
                    val id = valueElement.value
                    val named = symbolTable.lookupConst(id)

                    val isConstantOfCorrectType = named != null && named.type.trueType == expected

                    if (!isConstantOfCorrectType) {
                        throw IllegalStateException("Expected a value with type ${expected.name}")
                    }
                }

                else -> throw IllegalStateException("Expected a map literal, got: $valueElement")
            }
        }
    }

    companion object {
        @VisibleForTesting
        internal fun validate(symbolTable: SymbolTable, value: ConstValueElement, expected: ThriftType) {
            val trueType = expected.trueType
            Validators.forType(trueType).validate(symbolTable, trueType, value)
        }
    }
}
