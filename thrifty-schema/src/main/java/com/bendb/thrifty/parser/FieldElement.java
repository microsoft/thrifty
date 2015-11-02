package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class FieldElement {
    public static Builder builder(Location location) {
        return new AutoValue_FieldElement.Builder()
                .location(location)
                .documentation("");
    }

    public abstract Location location();
    public abstract String documentation();
    public abstract int fieldId();
    public abstract boolean required();
    public abstract String type();
    public abstract String name();
    @Nullable public abstract ConstValueElement constValue();

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder fieldId(int fieldId);
        Builder required(boolean required);
        Builder type(String type);
        Builder name(String name);
        Builder constValue(ConstValueElement constValue);

        FieldElement build();
    }
}
