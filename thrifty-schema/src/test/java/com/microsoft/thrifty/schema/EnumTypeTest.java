package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnumTypeTest {
    @Mock Program program;
    ImmutableMap<NamespaceScope, String> namespaces;

    EnumType enumType;

    @Before
    public void setup() {
        namespaces = ImmutableMap.of();

        when(program.namespaces()).thenReturn(namespaces);

        Location location = Location.get("", "");
        EnumMemberElement memberElement = new EnumMemberElement(location, "FOO", 1);

        EnumElement element = new EnumElement(
                location,
                "AnEnum",
                ImmutableList.of(memberElement));

        enumType = new EnumType(program, element);
    }

    @Test
    public void builderCreatesCorrectEnumType() {
        ImmutableList<EnumMember> members = ImmutableList.of();
        ImmutableMap<String, String> annotations = ImmutableMap.of();
        ImmutableMap<NamespaceScope, String> namespaces = ImmutableMap.of();

        EnumType builderEnumType = enumType.toBuilder()
                .members(members)
                .annotations(annotations)
                .namespaces(namespaces)
                .build();

        assertEquals(builderEnumType.members(), members);
        assertEquals(builderEnumType.annotations(), annotations);
        assertEquals(builderEnumType.namespaces(), namespaces);
    }

    @Test
    public void toBuilderCreatesCorrectEnumType() {
        assertEquals(enumType.toBuilder().build(), enumType);
    }
}
