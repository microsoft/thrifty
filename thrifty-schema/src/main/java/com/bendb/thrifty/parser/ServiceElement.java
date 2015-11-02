package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

@AutoValue
public abstract class ServiceElement {
    public abstract String documentation();
    public abstract Location location();
    public abstract String name();
    @Nullable public abstract String extendsServiceName();
    public abstract ImmutableList<FunctionElement> functions();
}
