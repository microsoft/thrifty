package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ScalarTypeElement extends TypeElement {
    ScalarTypeElement() {
    }

    public static ScalarTypeElement create(
            Location location,
            String name,
            AnnotationElement annotations) {
        return new AutoValue_ScalarTypeElement(location, name, annotations);
    }
}
