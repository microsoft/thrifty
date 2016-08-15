package com.microsoft.thrifty.schema;

import com.google.common.base.Joiner;

public class LoadFailedException extends Exception {
    private final ErrorReporter errorReporter;

    public LoadFailedException(Throwable cause, ErrorReporter errorReporter) {
        super(Joiner.on("\n").join(errorReporter.formattedReports()), cause);
        this.errorReporter = errorReporter;
    }

    public ErrorReporter errorReporter() {
        return errorReporter;
    }
}
