package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.bendb.thrifty.schema.ThriftType;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class TypedefElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String oldName();
    public abstract String newName();

    @Nullable
    public abstract ThriftType resolvedType();

    public boolean needsResolution() {
        return resolvedType() == null;
    }

    public TypedefElement withType(ThriftType type) {
        return new AutoValue_TypedefElement.Builder(this)
                .resolvedType(type)
                .build();
    }

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
        Builder resolvedType(ThriftType type);

        TypedefElement build();
    }
}
