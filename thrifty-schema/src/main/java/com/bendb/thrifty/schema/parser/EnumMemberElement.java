package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class EnumMemberElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String name();
    @Nullable public abstract Integer value();

    public static Builder builder(Location location) {
        return new AutoValue_EnumMemberElement.Builder()
                .location(location)
                .documentation("");
    }

    public EnumMemberElement withValue(Integer value) {
        return new AutoValue_EnumMemberElement.Builder(this)
                .value(value)
                .build();
    }

    EnumMemberElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder name(String name);
        Builder value(Integer value);

        EnumMemberElement build();
    }
}
