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
import com.google.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Represents an enumeration defined in Thrift IDL.
 */
public class EnumType extends UserType {
    private final ImmutableList<EnumMember> members;

    EnumType(Program program, EnumElement element) {
        super(program, new UserElementMixin(element));

        ImmutableList.Builder<EnumMember> builder = ImmutableList.builder();
        for (EnumMemberElement memberElement : element.members()) {
            builder.add(new EnumMember(memberElement));
        }
        this.members = builder.build();
    }

    private EnumType(Builder builder) {
        super(builder);
        this.members = builder.members;
    }

    public ImmutableList<EnumMember> members() {
        return members;
    }

    public EnumMember findMemberByName(String name) {
        for (EnumMember member : members) {
            if (member.name().equals(name)) {
                return member;
            }
        }
        throw new NoSuchElementException();
    }

    public EnumMember findMemberById(int id) {
        for (EnumMember member : members) {
            if (member.value() == id) {
                return member;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public boolean isEnum() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitEnum(this);
    }

    @Override
    ThriftType withAnnotations(Map<String, String> annotations) {
        return toBuilder()
                .annotations(merge(this.annotations(), annotations))
                .build();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder extends UserType.UserTypeBuilder<EnumType, Builder> {
        private ImmutableList<EnumMember> members;

        Builder(EnumType enumType) {
            super(enumType);
            this.members = enumType.members;
        }

        public Builder members(List<EnumMember> members) {
            Preconditions.checkNotNull(members, "members");
            this.members = ImmutableList.copyOf(members);
            return this;
        }

        @Override
        public EnumType build() {
            return new EnumType(this);
        }
    }
}
