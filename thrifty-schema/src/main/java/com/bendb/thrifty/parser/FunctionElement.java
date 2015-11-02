package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class FunctionElement {
    public abstract Location location();
    public abstract boolean oneWay();
    public abstract String documentation();
    public abstract String returnType();
    public abstract String name();
    public abstract ImmutableList<FieldElement> params();
    public abstract ImmutableList<FieldElement> exceptions();
}
