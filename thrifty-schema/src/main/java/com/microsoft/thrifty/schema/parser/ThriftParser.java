/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.schema.parser;

import com.google.common.base.Joiner;
import com.microsoft.thrifty.schema.ErrorReporter;
import com.microsoft.thrifty.schema.Location;
import com.microsoft.thrifty.schema.antlr.AntlrThriftLexer;
import com.microsoft.thrifty.schema.antlr.AntlrThriftParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.Locale;

public final class ThriftParser {

    /**
     * Parse the given Thrift {@code text}, using the given {@code location}
     * to anchor parsed elements withing the file.
     * @param location the {@link Location} of the data being parsed.
     * @param text the text to be parsed.
     * @return a representation of the parsed Thrift data.
     */
    public static ThriftFileElement parse(Location location, String text) {
        return parse(location, text, new ErrorReporter());
    }

    /**
     * Parse the given Thrift {@code text}, using the given {@code location}
     * to anchor parsed elements withing the file.
     * @param location the {@link Location} of the data being parsed.
     * @param text the text to be parsed.
     * @param reporter an {@link ErrorReporter} to collect warnings.
     * @return a representation of the parsed Thrift data.
     */
    public static ThriftFileElement parse(Location location, String text, ErrorReporter reporter) {
        ANTLRInputStream charStream = new ANTLRInputStream(text);
        AntlrThriftLexer lexer = new AntlrThriftLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        AntlrThriftParser antlrParser = new AntlrThriftParser(tokenStream);

        ThriftListener thriftListener = new ThriftListener(tokenStream, reporter, location);

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(thriftListener, antlrParser.document());

        if (reporter.hasError()) {
            String errorReports = Joiner.on('\n').join(reporter.formattedReports());
            String message = String.format(Locale.US, "Syntax errors in %s:\n%s", location, errorReports);
            throw new IllegalStateException(message);
        }

        return thriftListener.buildFileElement();
    }
}
