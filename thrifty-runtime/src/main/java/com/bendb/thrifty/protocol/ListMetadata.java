package com.bendb.thrifty.protocol;

public final class ListMetadata {
    public final byte elementTypeId;
    public final int size;

    public ListMetadata(byte elementTypeId, int size) {
        this.elementTypeId = elementTypeId;
        this.size = size;
    }
}
