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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;

import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

public class EnumType extends Named {
    private final EnumElement element;
    private final ThriftType type;
    private final ImmutableList<Member> members;
    private final ImmutableMap<String, String> annotations;

    @VisibleForTesting
    EnumType(EnumElement element, ThriftType type, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;

        ImmutableList.Builder<Member> membersBuilder = ImmutableList.builder();
        for (EnumMemberElement memberElement : element.members()) {
            membersBuilder.add(new Member(memberElement));
        }
        this.members = membersBuilder.build();

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    private EnumType(Builder builder) {
        super(builder.element.name(), builder.namespaces);
        this.element = builder.element;
        this.type = builder.type;
        this.members = builder.members;
        this.annotations = builder.annotations;
    }

    public String documentation() {
        return element.documentation();
    }

    @Override
    public Location location() {
        return element.location();
    }

    public ImmutableList<Member> members() {
        return members;
    }

    @Override
    public ThriftType type() {
        return type;
    }

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    public Member findMemberByName(String name) {
        for (Member member : members) {
            if (name.equals(member.name())) {
                return member;
            }
        }
        throw new NoSuchElementException();
    }

    public Member findMemberById(int id) {
        for (Member member : members) {
            if (member.value() == id) {
                return member;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public boolean isDeprecated() {
        return super.isDeprecated()
                || annotations.containsKey("deprecated")
                || annotations.containsKey("thrifty.deprecated");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnumType enumType = (EnumType) o;

        if (!element.equals(enumType.element)) {
            return false;
        }
        if (type != null ? !type.equals(enumType.type) : enumType.type != null) {
            return false;
        }
        if (members != null ? !members.equals(enumType.members) : enumType.members != null) {
            return false;
        }
        return annotations != null ? annotations.equals(enumType.annotations) : enumType.annotations == null;

    }

    @Override
    public int hashCode() {
        int result = element.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (members != null ? members.hashCode() : 0);
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        return result;
    }

    public Builder toBuilder() {
        return new Builder(element,
                type,
                members,
                annotations,
                namespaces());
    }

    public static final class Builder {
        private EnumElement element;
        private ThriftType type;
        private ImmutableList<Member> members;
        private ImmutableMap<String, String> annotations;
        private Map<NamespaceScope, String> namespaces;

        Builder(EnumElement element,
                       ThriftType type,
                       ImmutableList<Member> members,
                       ImmutableMap<String, String> annotations,
                       Map<NamespaceScope, String> namespaces) {
            this.element = element;
            this.type = type;
            this.members = members;
            this.annotations = annotations;
            this.namespaces = namespaces;
        }

        public Builder element(EnumElement element) {
            if (element == null) {
                throw new NullPointerException("element may not be null.");
            }
            this.element = element;
            return this;
        }

        public Builder type(ThriftType type) {
            this.type = type;
            return this;
        }

        public Builder members(ImmutableList<Member> members) {
            this.members = members;
            return this;
        }

        public Builder annotations(ImmutableMap<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder namespaces(Map<NamespaceScope, String> namespaces) {
            Map<NamespaceScope, String> immutableNamespaces = namespaces;
            if (!(immutableNamespaces instanceof ImmutableMap)) {
                immutableNamespaces = ImmutableMap.copyOf(namespaces);
            }
            this.namespaces = immutableNamespaces;
            return this;
        }

        public EnumType build() {
            return new EnumType(this);
        }
    }

    public static final class Member {
        private final EnumMemberElement element;
        private final ImmutableMap<String, String> annotations;

        Member(EnumMemberElement element) {
            this.element = element;

            ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
            AnnotationElement anno = element.annotations();
            if (anno != null) {
                annotationBuilder.putAll(anno.values());
            }
            this.annotations = annotationBuilder.build();
        }

        public String name() {
            return element.name();
        }

        public Integer value() {
            return element.value();
        }

        public String documentation() {
            return element.documentation();
        }

        public ImmutableMap<String, String> annotations() {
            return annotations;
        }

        public boolean hasJavadoc() {
            return JavadocUtil.hasJavadoc(this);
        }

        public boolean isDeprecated() {
            return annotations.containsKey("deprecated")
                    || annotations.containsKey("thrifty.deprecated")
                    || (hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@deprecated"));
        }

        @Override
        public String toString() {
            return name();
        }
    }
}
