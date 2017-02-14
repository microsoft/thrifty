package com.microsoft.thrifty.schema.parser;

import autovalue.shaded.com.google.common.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.ErrorReporter;
import com.microsoft.thrifty.schema.Location;
import com.microsoft.thrifty.schema.parser.ThriftListener;
import com.microsoft.thrifty.schema.antlr.AntlrThriftLexer;
import com.microsoft.thrifty.schema.antlr.AntlrThriftParser;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class ThriftListenerAntlrTest {
    @Test
    public void foo() {
        String thrift = "1: required string foo; // test\n// another test";
        ANTLRInputStream charStream = new ANTLRInputStream(thrift);
        AntlrThriftLexer lexer = new AntlrThriftLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        AntlrThriftParser parser = new AntlrThriftParser(tokenStream);

        AntlrThriftParser.FieldContext field = parser.field();

        System.out.println(field);
    }

    @Test
    public void testEnum() {
        String thrift = "// Some documentation\n" +
                "enum Foo {\n" +
                "  BAR = 1,\n" +
                "  BAZ, // wtf\n" +
                "  QUUX,\n" +
                "}";

        ANTLRInputStream charStream = new ANTLRInputStream(thrift);
        AntlrThriftLexer lexer = new AntlrThriftLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        AntlrThriftParser parser = new AntlrThriftParser(tokenStream);

        Location baseLocation = Location.get("foo", "bar");
        ThriftListener listener = new ThriftListener(tokenStream, new ErrorReporter(), baseLocation);

        AntlrThriftParser.EnumDefContext anEnum = parser.enumDef();
        ParseTreeWalker.DEFAULT.walk(listener, anEnum);

        EnumElement expected = EnumElement.builder(baseLocation.at(2, 1))
                .documentation("Some documentation\n")
                .name("Foo")
                .members(ImmutableList.<EnumMemberElement>builder()
                        .add(EnumMemberElement.builder(baseLocation.at(3, 3))
                                .name("BAR")
                                .value(1)
                                .build())
                        .add(EnumMemberElement.builder(baseLocation.at(4, 3))
                                .name("BAZ")
                                .value(2)
                                .documentation("wtf\n")
                                .build())
                        .add(EnumMemberElement.builder(baseLocation.at(5, 3))
                                .name("QUUX")
                                .value(3)
                                .build())
                        .build())
                .build();

        assertThat(listener.enums.get(0), equalTo(expected));
    }
}
