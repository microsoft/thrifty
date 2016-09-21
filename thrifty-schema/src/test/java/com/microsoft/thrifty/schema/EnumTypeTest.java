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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnumTypeTest {
    @Mock EnumElement element;
    @Mock ThriftType type;
    @Mock Map<NamespaceScope, String> namespaces;

    EnumType enumType;

    @Before
    public void setup() {
        when(element.name()).thenReturn("name");
        when(element.members()).thenReturn(ImmutableList.<EnumMemberElement>of());

        enumType = new EnumType(element, type, namespaces);
    }

    @Test
    public void builderCreatesCorrectEnumType() {
        EnumElement enumElement = mock(EnumElement.class);
        ThriftType thriftType = mock(ThriftType.class);
        ImmutableList<EnumType.Member> members = ImmutableList.of();
        ImmutableMap<String, String> annotations = ImmutableMap.of();
        Map<NamespaceScope, String> namespaces = new HashMap<>();

        EnumType builderEnumType = enumType.toBuilder()
                .element(enumElement)
                .type(thriftType)
                .members(members)
                .annotations(annotations)
                .namespaces(namespaces)
                .build();

        assertEquals(builderEnumType.type(), thriftType);
        assertEquals(builderEnumType.members(), members);
        assertEquals(builderEnumType.annotations(), annotations);
        assertEquals(builderEnumType.namespaces(), namespaces);
    }

    @Test
    public void toBuilderCreatesCorrectEnumType() {
        assertEquals(enumType.toBuilder().build(), enumType);
    }
}
