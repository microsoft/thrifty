package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;

import java.util.NoSuchElementException;

public class NewEnumType extends UserType {
    private final EnumElement element;
    private final ImmutableList<Member> members;

    NewEnumType(Program program, EnumElement element) {
        super(program, new UserElementMixin(element));
        this.element = element;

        ImmutableList.Builder<Member> builder = ImmutableList.builder();
        for (EnumMemberElement memberElement : element.members()) {
            builder.add(new Member(memberElement));
        }
        this.members = builder.build();
    }

    public ImmutableList<Member> members() {
        return members;
    }

    public Member findMemberByName(String name) {
        for (Member member : members) {
            if (member.name().equals(name)) {
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
    public boolean isEnum() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitEnum(this);
    }

    // TODO: add builder

    public static class Member implements UserElement {
        private final EnumMemberElement element;
        private final UserElementMixin mixin;

        private Member(EnumMemberElement element) {
            this.element = element;
            this.mixin = new UserElementMixin(element);
        }

        public int value() {
            return element.value();
        }

        @Override
        public String name() {
            return mixin.name();
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
}
