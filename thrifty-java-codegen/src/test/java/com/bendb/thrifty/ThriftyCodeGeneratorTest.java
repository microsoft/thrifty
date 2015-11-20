package com.bendb.thrifty;

import com.bendb.thrifty.parser.EnumElement;
import com.bendb.thrifty.parser.EnumMemberElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ThriftyCodeGeneratorTest {
    @Test
    public void enumGeneration() {
        Location location = Location.get("", "");
        EnumElement ee = EnumElement.builder(location)
                .documentation("A generated enum")
                .name("BuildStatus")
                .members(ImmutableList.of(
                        EnumMemberElement.builder(location)
                                .documentation("Represents a successful build")
                                .name("OK")
                                .value(0)
                                .build(),

                        EnumMemberElement.builder(location)
                                .documentation("Represents a failed build")
                                .name("FAIL")
                                .value(1)
                                .build()))
                .build();
        EnumType et = new EnumType(ee, ThriftType.get("BuildStatus"), ImmutableMap.of(NamespaceScope.JAVA, "com.test.enums"));

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(false);
        TypeSpec generated = gen.buildEnum(et);

        JavaFile file = JavaFile.builder("com.test.enums", generated).build();
        String code = file.toString();

        assertThat(code, is("test"));
    }
}