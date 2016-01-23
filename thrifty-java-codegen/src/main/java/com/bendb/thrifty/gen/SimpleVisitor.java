/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.gen;

import com.bendb.thrifty.schema.ThriftType;

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
    public T visitBool() {
        return visitBuiltin(ThriftType.BOOL);
    }

    @Override
    public T visitByte() {
        return visitBuiltin(ThriftType.BYTE);
    }

    @Override
    public T visitI16() {
        return visitBuiltin(ThriftType.I16);
    }

    @Override
    public T visitI32() {
        return visitBuiltin(ThriftType.I32);
    }

    @Override
    public T visitI64() {
        return visitBuiltin(ThriftType.I64);
    }

    @Override
    public T visitDouble() {
        return visitBuiltin(ThriftType.DOUBLE);
    }

    @Override
    public T visitString() {
        return visitBuiltin(ThriftType.STRING);
    }

    @Override
    public T visitBinary() {
        return visitBuiltin(ThriftType.BINARY);
    }

    @Override
    public T visitVoid() {
        return visitBuiltin(ThriftType.VOID);
    }
}
