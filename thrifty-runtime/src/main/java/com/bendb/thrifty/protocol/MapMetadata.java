package com.bendb.thrifty.protocol;

public class MapMetadata {
    public final byte keyTypeId;
    public final byte valueTypeId;
    public final int size;

    public MapMetadata(byte keyTypeId, byte valueTypeId, int size) {
        this.keyTypeId = keyTypeId;
        this.valueTypeId = valueTypeId;
        this.size = size;
    }
}
