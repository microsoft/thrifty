package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class FieldElement {
    public static Builder builder(Location location) {
        return new AutoValue_FieldElement.Builder()
                .location(location)
                .documentation("")
                .required(false);
    }

    public FieldElement withId(int fieldId) {
        return new AutoValue_FieldElement.Builder(this)
                .fieldId(fieldId)
                .build();
    }

    public abstract Location location();
    public abstract String documentation();
    @Nullable public abstract Integer fieldId();
    public abstract boolean required();
    public abstract String type();
    public abstract String name();
    @Nullable public abstract ConstValueElement constValue();

    FieldElement() {}

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder fieldId(Integer fieldId);
        Builder required(boolean required);
        Builder type(String type);
        Builder name(String name);
        Builder constValue(ConstValueElement constValue);

        FieldElement build();
    }
}
