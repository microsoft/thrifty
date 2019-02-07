package com.microsoft.thrifty.schema.parser

import com.microsoft.thrifty.schema.ErrorReporter
import com.microsoft.thrifty.schema.Location
import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

/**
 * Adapts [ErrorReporter] to the [ANTLRErrorListener] interface.
 */
internal class ParserErrorListener(
        private val location: Location,
        private val reporter: ErrorReporter
) : BaseErrorListener() {
    override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?) {
        // Antlr char positions are zero-based, location is not.
        val loc = location.at(line, charPositionInLine + 1)
        reporter.error(loc, msg)
    }
}