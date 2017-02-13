package com.microsoft.thrifty.schema.antlr;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.UnbufferedCharStream;
import org.junit.Test;

import java.io.StringReader;

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
}
