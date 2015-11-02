package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class IncludeElement {
    public abstract Location location();
    public abstract boolean isCpp();
    public abstract String path();

    IncludeElement() {}

    public static IncludeElement create(Location location, boolean isCpp, String path) {
        return new AutoValue_IncludeElement(location, isCpp, path);
    }
}
