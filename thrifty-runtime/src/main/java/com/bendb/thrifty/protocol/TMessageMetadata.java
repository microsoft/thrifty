package com.bendb.thrifty.protocol;

public final class TMessageMetadata {
    public final String name;
    public final byte type;
    public final int seqId;

    public TMessageMetadata(String name, byte type, int seqId) {
        this.name = name == null ? "" : name;
        this.type = type;
        this.seqId = seqId;
    }
}
