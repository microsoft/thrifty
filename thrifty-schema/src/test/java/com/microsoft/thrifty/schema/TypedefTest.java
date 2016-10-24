package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.TypedefElement;

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
public class TypedefTest {
    @Mock TypedefElement element;
    @Mock Map<NamespaceScope, String> namespaces;

    Typedef typedef;

    @Before
    public void setup() {
        when(element.newName()).thenReturn("name");
        typedef = new Typedef(element, namespaces);
    }

    @Test
    public void builderCreatesCorrectTypedef() {
        ImmutableMap<String, String> annotations = ImmutableMap.of();
        Map<NamespaceScope, String> namespace = mock(Map.class);
        ThriftType oldType = mock(ThriftType.class);

        Typedef typedef = this.typedef.toBuilder()
                .annotations(annotations)
                .namespaces(namespace)
                .oldType(oldType)
                .build();

        assertEquals(typedef.annotations(), annotations);
        assertEquals(typedef.oldType(), oldType);
        assertEquals(typedef.namespaces(), namespace);
    }

    @Test
    public void toBuilderCreatesCorrectTypedef() {
        assertEquals(typedef.toBuilder().build(), typedef);
    }
}
