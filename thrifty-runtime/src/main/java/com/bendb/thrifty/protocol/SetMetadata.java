package com.bendb.thrifty.protocol;

public final class SetMetadata {
    public final byte elementTypeId;
    public final int size;

    public SetMetadata(byte elementTypeId, int size) {
        this.elementTypeId = elementTypeId;
        this.size = size;
    }
}
