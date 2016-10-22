package com.microsoft.thrifty.schema;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import com.microsoft.thrifty.schema.parser.StructElement;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * A mixin encapsulating a common implementation of {@link UserElement},
 * which does not conveniently fit in a single base class.
 */
class UserElementMixin implements UserElement {
    private final String name;
    private final Location location;
    private final String documentation;
    private final ImmutableMap<String, String> annotations;

    UserElementMixin(StructElement struct) {
        this(struct.name(), struct.location(), struct.documentation(), struct.annotations());
    }

    UserElementMixin(EnumElement enumElement) {
        this(enumElement.name(), enumElement.location(), enumElement.documentation(), enumElement.annotations());
    }

    UserElementMixin(EnumMemberElement member) {
        this(member.name(), member.location(), member.documentation(), member.annotations());
    }

    UserElementMixin(
            String name,
            Location location,
            String documentation,
            @Nullable AnnotationElement annotationElement) {
        this.name = name;
        this.location = location;
        this.documentation = documentation;

        ImmutableMap.Builder<String, String> annotations = ImmutableMap.builder();
        if (annotationElement != null) {
            annotations.putAll(annotationElement.values());
        }
        this.annotations = annotations.build();
    }

    private UserElementMixin(Builder builder) {
        this.name = builder.name;
        this.location = builder.location;
        this.documentation = builder.documentation;
        this.annotations = builder.annotations.build();
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String documentation() {
        return documentation;
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    @Override
    public boolean hasJavadoc() {
        return JavadocUtil.isNonEmptyJavadoc(documentation());
    }

    @Override
    public boolean isDeprecated() {
        return annotations.containsKey("deprecated")
                || annotations.containsKey("thrifty.deprecated")
                || (hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@deprecated"));
    }

    @Override
    public String toString() {
        return "UserElementMixin{"
                + "name='" + name + '\''
                + ", location=" + location
                + ", documentation='" + documentation + '\''
                + ", annotations=" + annotations
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserElementMixin that = (UserElementMixin) o;

        if (!name.equals(that.name)) return false;
        if (!location.equals(that.location)) return false;
        if (!documentation.equals(that.documentation)) return false;
        return annotations.equals(that.annotations);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + documentation.hashCode();
        result = 31 * result + annotations.hashCode();
        return result;
    }

    Builder toBuilder() {
        return new Builder(this);
    }

    static class Builder {
        private String name;
        private Location location;
        private String documentation;
        private ImmutableMap.Builder<String, String> annotations;

        private Builder(UserElement userElement) {
            this.name = userElement.name();
            this.location = userElement.location();
            this.documentation = userElement.documentation();
            this.annotations = ImmutableMap.builder();

            this.annotations.putAll(userElement.annotations());
        }

        Builder name(String name) {
            this.name = Preconditions.checkNotNull(name, "name");
            return this;
        }

        Builder location(Location location) {
            this.location = Preconditions.checkNotNull(location, "name");
            return this;
        }

        Builder documentation(String documentation) {
            if (JavadocUtil.isNonEmptyJavadoc(documentation)) {
                this.documentation = documentation;
            } else {
                this.documentation = "";
            }
            return this;
        }

        Builder annotations(Map<String, String> annotations) {
            this.annotations.putAll(annotations);
            return this;
        }

        UserElementMixin build() {
            return new UserElementMixin(this);
        }
    }
}
