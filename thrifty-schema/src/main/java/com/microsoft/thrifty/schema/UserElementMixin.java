/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.schema;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.FunctionElement;
import com.microsoft.thrifty.schema.parser.ServiceElement;
import com.microsoft.thrifty.schema.parser.StructElement;
import com.microsoft.thrifty.schema.parser.TypedefElement;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * A mixin encapsulating a common implementation of {@link UserElement},
 * which does not conveniently fit in a single base class.
 */
class UserElementMixin implements UserElement {
    private final UUID uuid;
    private final String name;
    private final Location location;
    private final String documentation;
    private final ImmutableMap<String, String> annotations;

    UserElementMixin(StructElement struct) {
        this(struct.uuid(), struct.name(), struct.location(), struct.documentation(), struct.annotations());
    }

    UserElementMixin(FieldElement field) {
        this(field.uuid(), field.name(), field.location(), field.documentation(), field.annotations());
    }

    UserElementMixin(EnumElement enumElement) {
        this(enumElement.uuid(), enumElement.name(), enumElement.location(), enumElement.documentation(),
                enumElement.annotations());
    }

    UserElementMixin(EnumMemberElement member) {
        this(member.uuid(), member.name(), member.location(), member.documentation(), member.annotations());
    }

    UserElementMixin(TypedefElement element) {
        this(element.uuid(), element.newName(), element.location(), element.documentation(), element.annotations());
    }

    UserElementMixin(ServiceElement element) {
        this(element.uuid(), element.name(), element.location(), element.documentation(), element.annotations());
    }

    UserElementMixin(FunctionElement element) {
        this(element.uuid(), element.name(), element.location(), element.documentation(), element.annotations());
    }

    UserElementMixin(
            UUID uuid,
            String name,
            Location location,
            String documentation,
            @Nullable AnnotationElement annotationElement) {
        this.uuid = uuid;
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
        this.uuid = builder.uuid;
        this.name = builder.name;
        this.location = builder.location;
        this.documentation = builder.documentation;
        this.annotations = builder.annotations;
    }

    @Override
    public UUID uuid() {
        return uuid;
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
    public boolean isDeprecated() {
        return hasThriftOrJavadocAnnotation("deprecated");
    }

    /**
     * Checks for the presence of the given annotation name, in several possible
     * varieties.  Returns true if:
     *
     * <ul>
     *     <li>A Thrift annotation matching the exact name is present</li>
     *     <li>A Thrift annotation equal to the string "thrifty." plus the name is present</li>
     *     <li>The Javadoc contains "@" plus the annotation name</li>
     * </ul>
     *
     * The latter two conditions are officially undocumented, but are present for
     * legacy use.  This behavior is subject to change without notice!
     */
    boolean hasThriftOrJavadocAnnotation(String name) {
        return annotations().containsKey(name)
                || annotations().containsKey("thrifty." + name)
                || (hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@" + name));
    }

    @Override
    public String toString() {
        return "UserElementMixin{"
                + "uuid='" + uuid + '\''
                + ", name='" + name + '\''
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

        if (!uuid.equals(that.uuid)) return false;
        if (!name.equals(that.name)) return false;
        if (!location.equals(that.location)) return false;
        if (!documentation.equals(that.documentation)) return false;
        return annotations.equals(that.annotations);

    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + documentation.hashCode();
        result = 31 * result + annotations.hashCode();
        return result;
    }

    Builder toBuilder() {
        return new Builder(this);
    }

    static class Builder {
        private UUID uuid;
        private String name;
        private Location location;
        private String documentation;
        private ImmutableMap<String, String> annotations;

        private Builder(UserElement userElement) {
            this.uuid = userElement.uuid();
            this.name = userElement.name();
            this.location = userElement.location();
            this.documentation = userElement.documentation();
            this.annotations = userElement.annotations();
        }

        Builder uuid(UUID uuid) {
            this.uuid = Preconditions.checkNotNull(uuid, "uuid");
            return this;
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
            if (UserElement.isNonEmptyJavadoc(documentation)) {
                this.documentation = documentation;
            } else {
                this.documentation = "";
            }
            return this;
        }

        Builder annotations(Map<String, String> annotations) {
            this.annotations = ImmutableMap.copyOf(Preconditions.checkNotNull(annotations));
            return this;
        }

        UserElementMixin build() {
            return new UserElementMixin(this);
        }
    }
}
