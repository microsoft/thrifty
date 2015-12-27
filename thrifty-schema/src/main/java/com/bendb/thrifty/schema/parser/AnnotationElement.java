package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AutoValue
public abstract class AnnotationElement {
    public abstract Location location();
    public abstract ImmutableMap<String, String> values();

    @Nullable
    public String get(@Nonnull String name) {
        return values().get(name);
    }

    public boolean containsKey(@Nonnull String name) {
        return values().containsKey(name);
    }

    public boolean isEmpty() {
        return values().isEmpty();
    }

    public int size() {
        return values().size();
    }

    public static AnnotationElement create(Location location, Map<String, String> values) {
        return new AutoValue_AnnotationElement(location, ImmutableMap.copyOf(values));
    }
}
