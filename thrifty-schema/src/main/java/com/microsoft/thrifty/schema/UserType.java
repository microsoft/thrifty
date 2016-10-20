package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableMap;

public abstract class UserType extends NewThriftType implements UserElement {
    private final Program program;
    private final UserElementMixin mixin;

    UserType(Program program, UserElementMixin mixin) {
        super(mixin.name());
        this.program = program;
        this.mixin = mixin;
    }

    public String getNamespace(NamespaceScope namespace) {
        String ns = program.namespaces().get(namespace);
        if (ns == null && namespace != NamespaceScope.ALL) {
            ns = program.namespaces().get(NamespaceScope.ALL);
        }

        if (ns == null) {
            ns = "";
        }

        return ns;
    }

    public Program program() {
        return program;
    }

    @Override
    public Location location() {
        return mixin.location();
    }

    @Override
    public String documentation() {
        return mixin.documentation();
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return mixin.annotations();
    }

    @Override
    public boolean hasJavadoc() {
        return mixin.hasJavadoc();
    }

    @Override
    public boolean isDeprecated() {
        return mixin.isDeprecated();
    }
}
