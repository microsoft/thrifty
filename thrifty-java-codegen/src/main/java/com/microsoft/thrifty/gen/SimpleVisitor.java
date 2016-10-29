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
package com.microsoft.thrifty.gen;

import com.microsoft.thrifty.schema.BuiltinThriftType;
import com.microsoft.thrifty.schema.ThriftType;

/**
 * A visitor that collapses the various builtin visit methods into
 * one; useful when you don't need to distinguish between specific
 * builtin types.
 *
 * @param <T> the type of value returned by visit methods.
 */
abstract class SimpleVisitor<T> implements ThriftType.Visitor<T>  {

    public abstract T visitBuiltin(ThriftType builtinType);

    @Override
    public T visitBool(BuiltinThriftType boolType) {
        return visitBuiltin(boolType);
    }

    @Override
    public T visitByte(BuiltinThriftType byteType) {
        return visitBuiltin(byteType);
    }

    @Override
    public T visitI16(BuiltinThriftType i16Type) {
        return visitBuiltin(i16Type);
    }

    @Override
    public T visitI32(BuiltinThriftType i32Type) {
        return visitBuiltin(i32Type);
    }

    @Override
    public T visitI64(BuiltinThriftType i64Type) {
        return visitBuiltin(i64Type);
    }

    @Override
    public T visitDouble(BuiltinThriftType doubleType) {
        return visitBuiltin(doubleType);
    }

    @Override
    public T visitString(BuiltinThriftType stringType) {
        return visitBuiltin(stringType);
    }

    @Override
    public T visitBinary(BuiltinThriftType binaryType) {
        return visitBuiltin(binaryType);
    }

    @Override
    public T visitVoid(BuiltinThriftType voidType) {
        return visitBuiltin(voidType);
    }
}
