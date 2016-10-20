package com.microsoft.thrifty.schema;

public class MapType extends NewThriftType {
    private final NewThriftType keyType;
    private final NewThriftType valueType;

    MapType(NewThriftType keyType, NewThriftType valueType) {
        super("map<" + keyType.name() + "," + valueType.name() + ">");
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public NewThriftType keyType() {
        return keyType;
    }

    public NewThriftType valueType() {
        return valueType;
    }

    @Override
    public boolean isMap() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitMap(this);
    }
}
