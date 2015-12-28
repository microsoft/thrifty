package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SetTypeElement extends TypeElement {
    public abstract TypeElement elementType();

    SetTypeElement() {
    }

    public static SetTypeElement create(
            Location location,
            TypeElement element,
            AnnotationElement annotations) {
        String name = "set<" + element.name() + ">";
        return new AutoValue_SetTypeElement(location, name, annotations, element);
    }
}
