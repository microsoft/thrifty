/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
@file:JvmName("SchemaFunctionalEquality")

package com.bendb.thrifty.schema

/*
 * Functional ABI equality checking for Thrifty elements and Schemas. This exists because we can't
 * compare raw element types together due to misc metadata that their AutoValue representations
 * would report as differing but are not relevant to the actual ABI (location, formatting, etc).
 */

/**
 * For comparing docs, we remove stars as things get weird in the parser.
 */
private fun String.cleanedDoc(): String {
    return trim().replace("*", "").split("\n").joinToString("\n") { it.trimEnd() }
}

/**
 * A fully qualified class name of a given [UserType], used for equality checking.
 */
private inline val UserType.fqcn: String
    get() = "$javaPackage.$name"

/**
 * The java package name from the spec. We always assume its there because we don't support specs that don't.
 */
private inline val UserType.javaPackage: String
    get() = getNamespaceFor(NamespaceScope.JAVA)!!

/**
 * The java package name from the spec. We always assume its there because we don't support specs that don't.
 */
private inline val Constant.javaPackage: String
    get() {
        return getNamespaceFor(NamespaceScope.JAVA)!!
    }

/**
 * Checks that this [ThriftType] is equal to a given [other] [ThriftType].
 *
 * The following properties are checked:
 * - [ThriftType.annotations]
 * - [SetType.elementType]
 * - [ListType.elementType]
 * - [MapType.keyType]
 * - [MapType.valueType]
 * - [BuiltinType] are compared by equality
 * - [UserType] are compared by type checks, then fully qualified class name for linking. Set
 * [deepCheck] to enable deep comparisons on UserTypes too.
 *
 * @param other the other [ThriftType] to check
 * @param deepCheck a flag to signal whether or not the check should be deep. By default this is
 * `false` and UserTypes will only be compared by their fully qualified names (basically a linking
 * check).
 * @param lazyMessage a message to report if a check fails
 */
fun ThriftType.checkFunctionallyEquals(
    other: ThriftType,
    deepCheck: Boolean = false,
    lazyMessage: () -> String
) {
    check(annotations == other.annotations, lazyMessage)
    when (this) {
        is BuiltinType -> {
            check(this == other, lazyMessage)
        }
        is SetType -> {
            check(other is SetType)
            elementType.checkFunctionallyEquals(other.elementType, deepCheck, lazyMessage)
        }
        is ListType -> {
            check(other is ListType)
            elementType.checkFunctionallyEquals(other.elementType, deepCheck, lazyMessage)
        }
        is MapType -> {
            check(other is MapType)
            keyType.checkFunctionallyEquals(other.keyType, deepCheck, lazyMessage)
            valueType.checkFunctionallyEquals(other.valueType, deepCheck, lazyMessage)
        }
        is UserType -> {
            when (this) {
                is StructType -> {
                    check(other is StructType, lazyMessage)
                    check(fqcn == other.fqcn, lazyMessage)
                    if (deepCheck) {
                        checkFunctionallyEquals(other)
                    }
                }
                is EnumType -> {
                    check(other is EnumType, lazyMessage)
                    check(fqcn == other.fqcn, lazyMessage)
                    if (deepCheck) {
                        checkFunctionallyEquals(other)
                    }
                }
                is TypedefType -> {
                    check(other is TypedefType, lazyMessage)
                    check(fqcn == other.fqcn, lazyMessage)
                    if (deepCheck) {
                        checkFunctionallyEquals(other)
                    }
                }
                is ServiceType -> {
                    check(other is ServiceType, lazyMessage)
                    check(fqcn == other.fqcn, lazyMessage)
                    if (deepCheck) {
                        checkFunctionallyEquals(other)
                    }
                }
            }
        }
    }
}

/**
 * Checks that this [Field] is equal to a given [other] [Field].
 *
 * The following properties are checked:
 * - [Field.documentation]
 * - [Field.name]
 * - [Field.annotations]
 * - [Field.type]
 * - [Field.required]
 * - [Field.optional]
 *
 * @param other the other [Field] to check
 * @param prefix a contextual prefix to use in error messaging, as [Field]s can be used in
 * [StructType.fields], [ServiceMethod.parameters], and [ServiceMethod.exceptions].
 */
fun Field.checkFunctionallyEquals(other: Field, prefix: String) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "$prefix documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "$prefix name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(annotations == other.annotations) {
        "$prefix annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    type.checkFunctionallyEquals(other.type) {
        "$prefix type mismatch at $location. Found $type but expected ${other.type}"
    }
    check(required == other.required) {
        "$prefix required mismatch at $location. Found $required but expected ${other.required}"
    }
    check(optional == other.optional) {
        "$prefix optional mismatch at $location. Found $optional but expected ${other.optional}"
    }
}

/**
 * Checks that this [ServiceMethod] is equal to a given [other] [ServiceMethod].
 *
 * The following properties are checked:
 * - [ServiceMethod.documentation]
 * - [ServiceMethod.name]
 * - [ServiceMethod.annotations]
 * - [ServiceMethod.returnType]
 * - [ServiceMethod.parameters]
 * - [ServiceMethod.exceptions]
 *
 * @param other the other [StructType] to check
 */
fun ServiceMethod.checkFunctionallyEquals(other: ServiceMethod) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "Service method documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "Service method name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(annotations == other.annotations) {
        "Service method annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    returnType.checkFunctionallyEquals(other.returnType) {
        "Service method return type mismatch at $location. Found $returnType but expected ${other.returnType}"
    }
    parameters.zip(other.parameters)
        .forEach { (parameter1, parameter2) ->
            parameter1.checkFunctionallyEquals(parameter2, "Service method parameter")
        }
    exceptions.zip(other.exceptions)
        .forEach { (exception1, exception2) ->
            exception1.checkFunctionallyEquals(exception2, "Service method exception")
        }
}

/**
 * Checks that this [StructType] is equal to a given [other] [StructType].
 *
 * The following properties are checked:
 * - [StructType.documentation]
 * - [StructType.name]
 * - [StructType.namespaces]
 * - [StructType.annotations]
 * - [StructType.isUnion]
 * - [StructType.isException]
 * - [StructType.isStruct]
 * - [StructType.fields]
 *
 * @param other the other [StructType] to check
 */
fun StructType.checkFunctionallyEquals(other: StructType) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "Struct documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "Struct name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(namespaces == other.namespaces) {
        "Struct namespaces mismatch at $location. Found $namespaces but expected ${other.namespaces}"
    }
    check(annotations == other.annotations) {
        "Struct annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    check(isUnion == other.isUnion) {
        "Struct isUnion mismatch at $location. Found $isUnion but expected ${other.isUnion}"
    }
    check(isException == other.isException) {
        "Struct isException mismatch at $location. Found $isException but expected ${other.isException}"
    }
    check(isStruct == other.isStruct) {
        "Struct isStruct mismatch at $location. Found $isStruct but expected ${other.isStruct}"
    }
    fields.zip(other.fields)
        .forEach { (field1, field2) ->
            field1.checkFunctionallyEquals(field2, "Struct field")
        }
}

/**
 * Checks that this [ServiceType] is equal to a given [other] [ServiceType].
 *
 * The following properties are checked:
 * - [ServiceType.documentation]
 * - [ServiceType.name]
 * - [ServiceType.namespaces]
 * - [ServiceType.annotations]
 * - [ServiceType.methods]
 *
 * @param other the other [ServiceType] to check
 */
fun ServiceType.checkFunctionallyEquals(other: ServiceType) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "Service documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "Service name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(namespaces == other.namespaces) {
        "Service namespaces mismatch at $location. Found $namespaces but expected ${other.namespaces}"
    }
    check(annotations == other.annotations) {
        "Service annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    methods.zip(other.methods)
        .forEach { (method1, method2) ->
            method1.checkFunctionallyEquals(method2)
        }
}

/**
 * Checks that this [Constant] is equal to a given [other] [Constant].
 *
 * The following properties are checked:
 * - [Constant.documentation]
 * - [Constant.name]
 * - [Constant.namespaces]
 * - [Constant.annotations]
 * - [Constant.value]
 *
 * @param other the other [Constant] to check
 */
fun Constant.checkFunctionallyEquals(other: Constant) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "Constant documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "Constant name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(namespaces == other.namespaces) {
        "Constant namespaces mismatch at $location. Found $namespaces but expected ${other.namespaces}"
    }
    check(annotations == other.annotations) {
        "Constant annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    check(value == other.value) {
        "Constant value mismatch at $location. Found $value but expected ${other.value}"
    }
}

/**
 * Checks that this [EnumMember] is equal to a given [other] [EnumMember].
 *
 * The following properties are checked:
 * - [EnumMember.documentation]
 * - [EnumMember.name]
 * - [EnumMember.annotations]
 * - [EnumMember.value]
 *
 * @param other the other [EnumMember] to check
 */
fun EnumMember.checkFunctionallyEquals(other: EnumMember) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "Enum member documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "Enum member name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(annotations == other.annotations) {
        "Enum member annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    check(value == other.value) {
        "Enum member value mismatch at $location. Found $value but expected ${other.value}"
    }
}

/**
 * Checks that this [EnumType] is equal to a given [other] [EnumType].
 *
 * The following properties are checked:
 * - [EnumType.documentation]
 * - [EnumType.name]
 * - [EnumType.namespaces]
 * - [EnumType.annotations]
 * - [EnumType.members]
 *
 * @param other the other [EnumType] to check
 */
fun EnumType.checkFunctionallyEquals(other: EnumType) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "Enum documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "Enum name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(namespaces == other.namespaces) {
        "Enum namespaces mismatch at $location. Found $namespaces but expected ${other.namespaces}"
    }
    check(annotations == other.annotations) {
        "Enum annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    members.zip(other.members)
        .forEach { (member1, member2) ->
            member1.checkFunctionallyEquals(member2)
        }
}

/**
 * Checks that this [TypedefType] is equal to a given [other] [TypedefType].
 *
 * The following properties are checked:
 * - [TypedefType.documentation]
 * - [TypedefType.name]
 * - [TypedefType.namespaces]
 * - [TypedefType.annotations]
 * - [TypedefType.oldType]
 *
 * @param other the other [TypedefType] to check
 */
fun TypedefType.checkFunctionallyEquals(other: TypedefType) {
    check(documentation.cleanedDoc() == other.documentation.cleanedDoc()) {
        "Typedef documentation mismatch at $location. Found ${documentation.cleanedDoc()} but expected ${other.documentation.cleanedDoc()}"
    }
    check(name == other.name) {
        "Typedef name mismatch at $location. Found $name but expected ${other.name}"
    }
    check(namespaces == other.namespaces) {
        "Typedef namespaces mismatch at $location. Found $namespaces but expected ${other.namespaces}"
    }
    check(annotations == other.annotations) {
        "Typedef annotations mismatch at $location. Found $annotations but expected ${other.annotations}"
    }
    oldType.checkFunctionallyEquals(other.oldType) {
        "Typedef oldType mismatch at $location. Found $oldType but expected ${other.oldType}"
    }
}

/**
 * Checks that this [Schema] is equal to a given [other] [Schema]. Note that since elements are
 * effectively sets, they're sorted by their fully qualified class names and zipped with [other] for
 * consistency and matching.
 *
 * @param other the other [Schema] to check
 */
fun Schema.checkFunctionallyEquals(other: Schema) {
    structs.sortedBy(StructType::fqcn)
        .zip(other.structs.sortedBy(StructType::fqcn))
        .forEach { (struct1, struct2) ->
            struct1.checkFunctionallyEquals(struct2)
        }
    unions.sortedBy(StructType::fqcn)
        .zip(other.unions.sortedBy(StructType::fqcn))
        .forEach { (union1, union2) ->
            union1.checkFunctionallyEquals(union2)
        }
    enums.sortedBy(EnumType::fqcn)
        .zip(other.enums.sortedBy(EnumType::fqcn))
        .forEach { (enum1, enum2) ->
            enum1.checkFunctionallyEquals(enum2)
        }
    services.sortedBy(ServiceType::fqcn)
        .zip(other.services.sortedBy(ServiceType::fqcn))
        .forEach { (service1, service2) ->
            service1.checkFunctionallyEquals(service2)
        }
    typedefs.sortedBy(TypedefType::fqcn)
        .zip(other.typedefs.sortedBy(TypedefType::fqcn))
        .forEach { (typedef1, typedef2) ->
            typedef1.checkFunctionallyEquals(typedef2)
        }
    exceptions.sortedBy(StructType::fqcn)
        .zip(other.exceptions.sortedBy(StructType::fqcn))
        .forEach { (exception1, exception2) ->
            exception1.checkFunctionallyEquals(exception2)
        }
    constants.sortedBy { "${it.javaPackage}${it.name}" }
        .zip(other.constants.sortedBy { "${it.javaPackage}${it.name}" })
        .forEach { (constant1, constant2) ->
            constant1.checkFunctionallyEquals(constant2)
        }
}
