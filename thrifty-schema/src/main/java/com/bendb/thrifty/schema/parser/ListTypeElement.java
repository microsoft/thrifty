package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ListTypeElement extends TypeElement {
    public abstract TypeElement elementType();

    ListTypeElement() {
    }

    public static ListTypeElement create(
            Location location,
            TypeElement element,
            AnnotationElement annotations) {
        String name = "list<" + element.name() + ">";
        return new AutoValue_ListTypeElement(location, name, annotations, element);
    }
}
