package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents one of the three aggregate Thrift declarations: struct, union, or exception.
 */
@AutoValue
public abstract class StructElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract Type type();
    public abstract String name();
    public abstract ImmutableList<FieldElement> fields();

    public static Builder builder(Location location) {
        return new AutoValue_StructElement.Builder()
                .location(location)
                .documentation("");
    }

    StructElement() {}

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
