package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class IncludeElement {
    public abstract Location location();
    public abstract boolean isCpp();
    public abstract String path();

    IncludeElement() {}

    public static IncludeElement create(Location location, boolean isCpp, String path) {
        return new AutoValue_IncludeElement(location, isCpp, path);
    }
}
