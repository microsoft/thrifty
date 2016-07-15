package com.microsoft.thrifty;

public interface Struct<T, B extends StructBuilder<T>> {
    Adapter<T, B> getAdapter();
}
