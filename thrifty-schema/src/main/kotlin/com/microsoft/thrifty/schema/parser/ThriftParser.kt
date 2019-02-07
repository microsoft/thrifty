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
package com.microsoft.thrifty.schema.parser

import com.microsoft.thrifty.schema.ErrorReporter
import com.microsoft.thrifty.schema.Location
import com.microsoft.thrifty.schema.antlr.AntlrThriftLexer
import com.microsoft.thrifty.schema.antlr.AntlrThriftParser
import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ConsoleErrorListener
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.util.Locale

/**
 * A simple driver for the generated ANTLR Thrift parser; parses single
 * .thrift documents.
 */
object ThriftParser {

    /**
     * Parse the given Thrift [text], using the given [location] to anchor
     * parsed elements within the file.
     *
     * @param location the [Location] of the file containing the given [text].
     * @param text the Thrift text to be parsed.
     * @param reporter an [ErrorReporter] to which parse errors will be reported.
     * @return a [ThriftFileElement] containing all Thrift elements in the input text.
     */
    fun parse(location: Location, text: String, reporter: ErrorReporter = ErrorReporter()): ThriftFileElement {
        val errorListener = ParserErrorListener(location, reporter)
        val charStream = CharStreams.fromString(text, location.path)
        val lexer = AntlrThriftLexer(charStream).withErrorReporting(errorListener)
        val tokenStream = CommonTokenStream(lexer)
        val antlrParser = AntlrThriftParser(tokenStream).withErrorReporting(errorListener)
        val documentParseTree = antlrParser.document()

        val thriftListener = ThriftListener(tokenStream, reporter, location)

        ParseTreeWalker.DEFAULT.walk(thriftListener, documentParseTree)

        if (reporter.hasError) {
            val errorReports = reporter.formattedReports().joinToString("\n")
            val message = String.format(Locale.US, "Syntax errors in %s:\n%s", location, errorReports)
            throw IllegalStateException(message)
        }

        return thriftListener.buildFileElement()
    }

    private fun <T : Recognizer<*, *>> T.withErrorReporting(errorListener: ANTLRErrorListener): T {
        removeErrorListener(ConsoleErrorListener.INSTANCE)
        addErrorListener(errorListener)
        return this
    }
}
