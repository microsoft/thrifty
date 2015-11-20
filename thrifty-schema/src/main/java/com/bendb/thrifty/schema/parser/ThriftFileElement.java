package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class ThriftFileElement {
    public abstract Location location();
    public abstract ImmutableList<NamespaceElement> namespaces();
    public abstract ImmutableList<IncludeElement> includes();
    public abstract ImmutableList<ConstElement> constants();
    public abstract ImmutableList<TypedefElement> typedefs();
    public abstract ImmutableList<EnumElement> enums();
    public abstract ImmutableList<StructElement> structs();
    public abstract ImmutableList<StructElement> unions();
    public abstract ImmutableList<StructElement> exceptions();
    public abstract ImmutableList<ServiceElement> services();

    public static Builder builder(Location location) {
        return new AutoValue_ThriftFileElement.Builder()
                .location(location)
                .namespaces(ImmutableList.<NamespaceElement>of())
                .includes(ImmutableList.<IncludeElement>of())
                .constants(ImmutableList.<ConstElement>of())
                .typedefs(ImmutableList.<TypedefElement>of())
                .enums(ImmutableList.<EnumElement>of())
                .structs(ImmutableList.<StructElement>of())
                .unions(ImmutableList.<StructElement>of())
                .exceptions(ImmutableList.<StructElement>of())
                .services(ImmutableList.<ServiceElement>of());
    }

    ThriftFileElement() {}

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder namespaces(ImmutableList<NamespaceElement> namespaces);
        Builder includes(ImmutableList<IncludeElement> includes);
        Builder constants(ImmutableList<ConstElement> constants);
        Builder typedefs(ImmutableList<TypedefElement> typedefs);
        Builder enums(ImmutableList<EnumElement> enums);
        Builder structs(ImmutableList<StructElement> structs);
        Builder unions(ImmutableList<StructElement> unions);
        Builder exceptions(ImmutableList<StructElement> exceptions);
        Builder services(ImmutableList<ServiceElement> services);

        ThriftFileElement build();
    }
}
