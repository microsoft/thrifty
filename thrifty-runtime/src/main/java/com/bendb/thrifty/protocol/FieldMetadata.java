package com.bendb.thrifty.protocol;

public final class FieldMetadata {
    public final String name;
    public final byte typeId;
    public final short fieldId;

    public FieldMetadata(String name, byte typeId, short fieldId) {
        this.name = name;
        this.typeId = typeId;
        this.fieldId = fieldId;
    }
}
