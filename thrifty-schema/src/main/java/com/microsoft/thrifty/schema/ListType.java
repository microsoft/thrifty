package com.microsoft.thrifty.schema;

public class ListType extends NewThriftType {
    private final NewThriftType elementType;

    ListType(NewThriftType elementType) {
        super("list<" + elementType.name() + ">");
        this.elementType = elementType;
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitList(this);
    }
}
