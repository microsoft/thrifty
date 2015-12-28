package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;

import javax.annotation.Nullable;

public abstract class TypeElement {
    public abstract Location location();
    public abstract String name();

    @Nullable
    public abstract AnnotationElement annotations();

    public static TypeElement scalar(Location location, String name, AnnotationElement annotations) {
        return ScalarTypeElement.create(location, name, annotations);
    }

    public static TypeElement list(
            Location location,
            TypeElement elementType,
            AnnotationElement annotations) {
        return ListTypeElement.create(location, elementType, annotations);
    }

    public static TypeElement set(
            Location location,
            TypeElement elementType,
            AnnotationElement annotations) {
        return SetTypeElement.create(location, elementType, annotations);
    }

    public static TypeElement map(
            Location location,
            TypeElement key,
            TypeElement value,
            AnnotationElement annotations) {
        return MapTypeElement.create(location, key, value, annotations);
    }
}
