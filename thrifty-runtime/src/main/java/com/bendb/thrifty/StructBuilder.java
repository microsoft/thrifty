package com.bendb.thrifty;

public interface StructBuilder<T> {
    T build();
    void reset();
}
