package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.StructElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StructTypeTest {
    @Mock StructElement element;
    @Mock ThriftType thriftType;
    @Mock Map<NamespaceScope, String> namespaces;
    @Mock FieldNamingPolicy fieldNamingPolicy;

    StructType structType;

    @Before
    public void setup() {
        when(element.name()).thenReturn("name");
        when(element.fields()).thenReturn(ImmutableList.<FieldElement>of());

        structType = new StructType(element, thriftType, namespaces, fieldNamingPolicy);
    }

    @Test
    public void builderCreatesCorrectStructType() {
        ThriftType thriftType = mock(ThriftType.class);
        ImmutableList<Field> fields = mock(ImmutableList.class);
        ImmutableMap<NamespaceScope, String> namespaces = mock(ImmutableMap.class);
        ImmutableMap<String, String> annotations = mock(ImmutableMap.class);

        StructType builderStructType = structType.toBuilder()
                .type(thriftType)
                .fields(fields)
                .annotations(annotations)
                .namespaces(namespaces)
                .build();

        assertEquals(builderStructType.type(), thriftType);
        assertEquals(builderStructType.fields(), fields);
        assertEquals(builderStructType.namespaces(), namespaces);
        assertEquals(builderStructType.annotations(), annotations);
    }

    @Test
    public void toBuilderCreatesCorrectStructType() {
        assertEquals(structType.toBuilder().build(), structType);
    }
}
