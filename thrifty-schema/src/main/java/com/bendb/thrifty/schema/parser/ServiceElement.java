package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

@AutoValue
public abstract class ServiceElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String name();
    @Nullable public abstract String extendsServiceName();
    public abstract ImmutableList<FunctionElement> functions();

    ServiceElement() { }

    public static Builder builder(Location location) {
        return new AutoValue_ServiceElement.Builder()
                .location(location)
                .documentation("")
                .functions(ImmutableList.<FunctionElement>of());
    }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder name(String name);
        Builder extendsServiceName(String serviceName);
        Builder functions(ImmutableList<FunctionElement> functions);

        ServiceElement build();
    }
}
