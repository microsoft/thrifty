package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TypedefElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String oldName();
    public abstract String newName();

    TypedefElement() {}

    public static Builder builder(Location location) {
        return new AutoValue_TypedefElement.Builder()
                .location(location)
                .documentation("");
    }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder oldName(String oldName);
        Builder newName(String newName);

        TypedefElement build();
    }
}
