package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SchemaTest {
    @Mock Iterable<Program> programs;
    @Mock Iterator<Program> programIterator;

    Schema schema;

    @Before
    public void setup() {
        when(programs.iterator()).thenReturn(programIterator);
        schema = new Schema(programs);
    }

    @Test
    public void builderCreatesCorrectSchema() {
        ImmutableList<StructType> structTypes = ImmutableList.of();
        ImmutableList<EnumType> enumTypes = ImmutableList.of();
        ImmutableList<Constant> constants = ImmutableList.of();
        ImmutableList<TypedefType> typedefs = ImmutableList.of();
        ImmutableList<ServiceType> services = ImmutableList.of();

        Schema.Builder builder = schema.toBuilder();
        builder.structs(structTypes);
        builder.unions(structTypes);
        builder.exceptions(structTypes);
        builder.enums(enumTypes);
        builder.constants(constants);
        builder.typedefs(typedefs);
        builder.services(services);
        Schema builtSchema = builder.build();

        assertEquals(builtSchema.structs(), structTypes);
        assertEquals(builtSchema.unions(), structTypes);
        assertEquals(builtSchema.exceptions(), structTypes);
        assertEquals(builtSchema.enums(), enumTypes);
        assertEquals(builtSchema.constants(), constants);
        assertEquals(builtSchema.typedefs(), typedefs);
        assertEquals(builtSchema.services(), services);
    }

    @Test
    public void toBuilderCreatesCorrectSchema() {
        assertEquals(schema.toBuilder().build(), schema);
    }
}
