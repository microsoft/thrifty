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

import com.microsoft.thrifty.schema.BuiltinType;
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
    public T visitBool(BuiltinType boolType) {
        return visitBuiltin(boolType);
    }

    @Override
    public T visitByte(BuiltinType byteType) {
        return visitBuiltin(byteType);
    }

    @Override
    public T visitI16(BuiltinType i16Type) {
        return visitBuiltin(i16Type);
    }

    @Override
    public T visitI32(BuiltinType i32Type) {
        return visitBuiltin(i32Type);
    }

    @Override
    public T visitI64(BuiltinType i64Type) {
        return visitBuiltin(i64Type);
    }

    @Override
    public T visitDouble(BuiltinType doubleType) {
        return visitBuiltin(doubleType);
    }

    @Override
    public T visitString(BuiltinType stringType) {
        return visitBuiltin(stringType);
    }

    @Override
    public T visitBinary(BuiltinType binaryType) {
        return visitBuiltin(binaryType);
    }

    @Override
    public T visitVoid(BuiltinType voidType) {
        return visitBuiltin(voidType);
    }
}
