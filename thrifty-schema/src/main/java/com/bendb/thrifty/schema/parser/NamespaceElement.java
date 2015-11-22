package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.bendb.thrifty.schema.NamespaceScope;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NamespaceElement {
    public abstract Location location();
    public abstract NamespaceScope scope();
    public abstract String namespace();

    NamespaceElement() { }

    public static Builder builder(Location location) {
        return new AutoValue_NamespaceElement.Builder()
                .location(location);
    }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder scope(NamespaceScope scope);
        Builder namespace(String namespace);

        NamespaceElement build();
    }
}
