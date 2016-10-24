package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.FunctionElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceMethodTest {
    @Mock FunctionElement functionElement;
    @Mock FieldNamingPolicy fieldNamingPolicy;

    ServiceMethod serviceMethod;

    @Before
    public void setup() {
        when(functionElement.params()).thenReturn(ImmutableList.<FieldElement>of());
        when(functionElement.exceptions()).thenReturn(ImmutableList.<FieldElement>of());
        serviceMethod = new ServiceMethod(functionElement, fieldNamingPolicy);
    }

    @Test
    public void builderCreatesCorrectServiceMethod() {
        ImmutableList<Field> fields = ImmutableList.of();
        ImmutableMap<String, String> annotations = ImmutableMap.of();
        ThriftType thriftType = mock(ThriftType.class);

        ServiceMethod builderServiceMethod = serviceMethod.toBuilder()
                .paramTypes(fields)
                .exceptionTypes(fields)
                .annotations(annotations)
                .returnType(thriftType)
                .build();

        assertEquals(builderServiceMethod.paramTypes(), fields);
        assertEquals(builderServiceMethod.returnType(), thriftType);
        assertEquals(builderServiceMethod.exceptionTypes(), fields);
        assertEquals(builderServiceMethod.annotations(), annotations);
    }

    @Test
    public void toBuilderCreatesCorrectServiceMethod() {
        assertEquals(serviceMethod.toBuilder().build(), serviceMethod);
    }
}
