package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableMap;

import java.util.Objects;

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

    abstract void link(Linker linker);

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

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        UserType that = (UserType) o;
        if (!mixin.equals(that.mixin)) return false;
        if (!program.equals(that.program)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mixin, program);
    }
}
