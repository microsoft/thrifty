package com.microsoft.thrifty.schema;

public class SetType extends NewThriftType {
    private final NewThriftType elementType;

    SetType(NewThriftType elementType) {
        super("set<" + elementType.name() + ">");
        this.elementType = elementType;
    }

    public NewThriftType elementType() {
        return elementType;
    }

    @Override
    public boolean isSet() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitSet(this);
    }
}
