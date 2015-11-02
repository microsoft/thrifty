package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.bendb.thrifty.NamespaceScope;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class NamespaceElement {
    public abstract Location location();
    public abstract NamespaceScope scope();
    public abstract String namespace();

    NamespaceElement() {}

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
