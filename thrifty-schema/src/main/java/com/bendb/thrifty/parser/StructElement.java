package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
abstract class StructElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract Type type();
    public abstract String name();
    public abstract ImmutableList<FieldElement> fields();

    public static Builder struct(Location location) {
        return builder(location).type(Type.STRUCT);
    }

    public static Builder union(Location location) {
        return builder(location).type(Type.UNION);
    }

    public static Builder exception(Location location) {
        return builder(location).type(Type.EXCEPTION);
    }

    private static Builder builder(Location location) {
        return new AutoValue_StructElement.Builder()
                .location(location)
                .documentation("");
    }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder type(Type type);
        Builder name(String name);
        Builder fields(ImmutableList<FieldElement> fields);

        StructElement build();
    }

    public enum Type {
        STRUCT,
        UNION,
        EXCEPTION
    }
}
