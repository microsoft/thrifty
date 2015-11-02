package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
abstract class EnumElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String name();
    public abstract ImmutableList<EnumMemberElement> members();

    EnumElement() {}

    public static Builder builder(Location location) {
        return new AutoValue_EnumElement.Builder()
                .location(location)
                .documentation("");
    }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder name(String name);
        Builder members(ImmutableList<EnumMemberElement> members);

        EnumElement build();
    }
}
