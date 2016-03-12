package com.microsoft.thrifty.schema;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class ErrorReporter {
    private boolean hasError = false;
    private List<Report> reports = new ArrayList<>();

    public void warn(Location location, String message) {
        reports.add(Report.create(Level.WARNING, location, message));
    }

    public void error(Location location, String message) {
        hasError = true;
        reports.add(Report.create(Level.ERROR, location, message));
    }

    boolean hasError() {
        return hasError;
    }

    public ImmutableList<Report> reports() {
        return ImmutableList.copyOf(reports);
    }

    public ImmutableList<String> formattedReports() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        StringBuilder sb = new StringBuilder();
        for (Report report : reports) {
            switch (report.level()) {
                case WARNING: sb.append("W: "); break;
                case ERROR:   sb.append("E: "); break;
                default:
                    throw new AssertionError("Unexpected report level: " + report.level());
            }

            sb.append(report.message());
            sb.append(" (at ");
            sb.append(report.location());
            sb.append(")");

            builder.add(sb.toString());

            sb.setLength(0);
        }
        return builder.build();
    }

    @AutoValue
    abstract static class Report {
        public abstract Level level();
        public abstract Location location();
        public abstract String message();

        static Report create(Level level, Location location, String message) {
            return new AutoValue_ErrorReporter_Report(level, location, message);
        }
    }

    public enum Level {
        WARNING,
        ERROR
    }
}
