package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ConstElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String type();
    public abstract String name();
    public abstract ConstValueElement value();

    public static Builder builder(Location location) {
        return new AutoValue_ConstElement.Builder()
                .location(location)
                .documentation("");
    }

    ConstElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder type(String type);
        Builder name(String name);
        Builder value(ConstValueElement value);

        ConstElement build();
    }
}
