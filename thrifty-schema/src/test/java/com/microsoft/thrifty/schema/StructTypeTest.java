package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.StructElement;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class StructTypeTest {
    @Mock StructElement element;
    @Mock ThriftType thriftType;
    @Mock Program program;

    StructType structType;

    @Before
    public void setup() {
        when(program.namespaces()).thenReturn(ImmutableMap.<NamespaceScope, String>of());

        when(element.name()).thenReturn("name");
        when(element.fields()).thenReturn(ImmutableList.<FieldElement>of());

        structType = new StructType(program, element);
    }

    @Test
    public void builderCreatesCorrectStructType() {
        ImmutableList<Field> fields = mock(ImmutableList.class);
        ImmutableMap<NamespaceScope, String> namespaces = mock(ImmutableMap.class);
        ImmutableMap<String, String> annotations = mock(ImmutableMap.class);

        StructType builderStructType = structType.toBuilder()
                .fields(fields)
                .annotations(annotations)
                .namespaces(namespaces)
                .build();

        assertEquals(builderStructType.fields(), fields);
        assertEquals(builderStructType.namespaces(), namespaces);
        assertEquals(builderStructType.annotations(), annotations);
    }

    @Test
    public void toBuilderCreatesCorrectStructType() {
        assertEquals(structType.toBuilder().build(), structType);
    }
}
