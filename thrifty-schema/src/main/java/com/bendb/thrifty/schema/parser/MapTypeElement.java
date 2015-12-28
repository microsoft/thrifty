package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MapTypeElement extends TypeElement {
    public abstract TypeElement keyType();
    public abstract TypeElement valueType();

    MapTypeElement() {
    }

    public static MapTypeElement create(
            Location location,
            TypeElement key,
            TypeElement value,
            AnnotationElement annotations) {
        String name = "map<" + key.name() + ", " + value.name() + ">";
        return new AutoValue_MapTypeElement(location, name, annotations, key, value);
    }
}
