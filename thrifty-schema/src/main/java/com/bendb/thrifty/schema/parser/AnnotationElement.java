package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@AutoValue
public abstract class AnnotationElement {
    public abstract Location location();
    public abstract ImmutableMap<String, String> annotations();

    public static AnnotationElement create(Location location, Map<String, String> annotations) {
        return new AutoValue_AnnotationElement(location, ImmutableMap.copyOf(annotations));
    }
}
