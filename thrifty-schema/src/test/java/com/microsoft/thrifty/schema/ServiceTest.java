package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.FunctionElement;
import com.microsoft.thrifty.schema.parser.ServiceElement;

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
public class ServiceTest {
    @Mock ServiceElement serviceElement;
    @Mock ThriftType thriftType;
    @Mock Map<NamespaceScope, String> namespaces;
    @Mock FieldNamingPolicy fieldNamingPolicy;

    Service service;

    @Before
    public void setup() {
        when(serviceElement.name()).thenReturn("name");
        when(serviceElement.functions()).thenReturn(ImmutableList.<FunctionElement>of());
        service = new Service(serviceElement, thriftType, namespaces, fieldNamingPolicy);
    }

    @Test
    public void builderCreatesCorrectService() {
        ImmutableList<ServiceMethod> methods = ImmutableList.of();
        ThriftType type = mock(ThriftType.class);
        ImmutableMap<String, String> annotations = ImmutableMap.of();
        Map<NamespaceScope, String> namespaces = new HashMap<>();

        Service builderService = service.toBuilder()
                .methods(methods)
                .type(type)
                .annotations(annotations)
                .namespaces(namespaces)
                .extendsService(type)
                .build();

        assertEquals(methods, builderService.methods());
        assertEquals(type, builderService.type());
        assertEquals(type, builderService.extendsService());
        assertEquals(annotations, builderService.annotations());
        assertEquals(namespaces, builderService.namespaces());
    }

    @Test
    public void toBuilderCreatesCorrectService() {
        assertEquals(service.toBuilder().build(), service);
    }
}
