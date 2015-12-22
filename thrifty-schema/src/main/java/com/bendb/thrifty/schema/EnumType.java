/*
 * Copyright (C) 2015 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.EnumElement;
import com.bendb.thrifty.schema.parser.EnumMemberElement;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.util.Map;
import java.util.NoSuchElementException;

public class EnumType extends Named {
    private final EnumElement element;
    private final ThriftType type;
    private final ImmutableList<Member> members;

    @VisibleForTesting
    public EnumType(EnumElement element, ThriftType type, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;

        ImmutableList.Builder<Member> membersBuilder = ImmutableList.builder();
        for (EnumMemberElement memberElement : element.members()) {
            membersBuilder.add(new Member(memberElement));
        }
        this.members = membersBuilder.build();
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

    public static final class Member {
        private final EnumMemberElement element;

        Member(EnumMemberElement element) {
            this.element = element;
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

        public boolean hasJavadoc() {
            return JavadocUtil.hasJavadoc(this);
        }

        @Override
        public String toString() {
            return name();
        }
    }
}
