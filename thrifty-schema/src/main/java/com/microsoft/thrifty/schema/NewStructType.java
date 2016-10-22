package com.microsoft.thrifty.schema;

import com.microsoft.thrifty.schema.parser.StructElement;

public class NewStructType extends UserType {


    NewStructType(Program program, StructElement element) {
        super(program, new UserElementMixin(element));
    }

    @Override
    public boolean isStruct() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitStruct(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }


    }
}
