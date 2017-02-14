package com.microsoft.thrifty.schema.antlr;

import com.microsoft.thrifty.schema.ErrorReporter;
import com.microsoft.thrifty.schema.Location;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.UnbufferedCharStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
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

    @Test
    public void testEnum() {
        String thrift = "enum Foo {\n" +
                "  BAR = 1,\n" +
                "  BAZ, // wtf\n" +
                "  QUUX,\n" +
                "}";

        ANTLRInputStream charStream = new ANTLRInputStream(thrift);
        AntlrThriftLexer lexer = new AntlrThriftLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        AntlrThriftParser parser = new AntlrThriftParser(tokenStream);

        ThriftListener listener = new ThriftListener(tokenStream, new ErrorReporter(), Location.get("foo", "bar"));

        AntlrThriftParser.T_enumContext anEnum = parser.t_enum();
        ParseTreeWalker.DEFAULT.walk(listener, anEnum);
    }
}
