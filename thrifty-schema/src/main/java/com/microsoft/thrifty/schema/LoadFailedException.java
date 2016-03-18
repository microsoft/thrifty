package com.microsoft.thrifty.schema;

public class LoadFailedException extends Exception {
    private final ErrorReporter errorReporter;

    public LoadFailedException(Throwable cause, ErrorReporter errorReporter) {
        super(cause);
        this.errorReporter = errorReporter;
    }

    public ErrorReporter errorReporter() {
        return errorReporter;
    }
}
