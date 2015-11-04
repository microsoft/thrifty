package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class FunctionElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract boolean oneWay();
    public abstract String returnType();
    public abstract String name();
    public abstract ImmutableList<FieldElement> params();
    public abstract ImmutableList<FieldElement> exceptions();

    public static Builder builder(Location location) {
        return new AutoValue_FunctionElement.Builder()
                .location(location)
                .documentation("")
                .params(ImmutableList.<FieldElement>of())
                .exceptions(ImmutableList.<FieldElement>of());
    }

    FunctionElement() {}

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder oneWay(boolean oneWay);
        Builder returnType(String returnType);
        Builder name(String name);
        Builder params(ImmutableList<FieldElement> params);
        Builder exceptions(ImmutableList<FieldElement> params);

        FunctionElement build();
    }
}
