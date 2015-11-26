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
