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
package com.bendb.thrifty.gen

import com.bendb.thrifty.schema.BuiltinType
import com.bendb.thrifty.schema.ThriftType

/**
 * A visitor that collapses the various builtin visit methods into
 * one; useful when you don't need to distinguish between specific
 * builtin types.
 *
 * @param T the type of value returned by visit methods.
 */
internal abstract class SimpleVisitor<T> : ThriftType.Visitor<T> {

    abstract fun visitBuiltin(builtinType: ThriftType): T

    override fun visitBool(boolType: BuiltinType): T {
        return visitBuiltin(boolType)
    }

    override fun visitByte(byteType: BuiltinType): T {
        return visitBuiltin(byteType)
    }

    override fun visitI16(i16Type: BuiltinType): T {
        return visitBuiltin(i16Type)
    }

    override fun visitI32(i32Type: BuiltinType): T {
        return visitBuiltin(i32Type)
    }

    override fun visitI64(i64Type: BuiltinType): T {
        return visitBuiltin(i64Type)
    }

    override fun visitDouble(doubleType: BuiltinType): T {
        return visitBuiltin(doubleType)
    }

    override fun visitString(stringType: BuiltinType): T {
        return visitBuiltin(stringType)
    }

    override fun visitBinary(binaryType: BuiltinType): T {
        return visitBuiltin(binaryType)
    }

    override fun visitVoid(voidType: BuiltinType): T {
        return visitBuiltin(voidType)
    }
}
