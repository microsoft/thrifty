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
package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public boolean hasError() {
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

    static class Report {
        private final Level level;
        private final Location location;
        private final String message;

        public Level level() {
            return level;
        }

        public Location location() {
            return location;
        }

        public String message() {
            return message;
        }

        private Report(Level level, Location location, String message) {
            this.level = level;
            this.location = location;
            this.message = message;
        }

        static Report create(Level level, Location location, String message) {
            return new Report(level, location, message);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Report report = (Report) o;
            return level == report.level
                    && Objects.equals(location, report.location)
                    && Objects.equals(message, report.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, location, message);
        }
    }

    public enum Level {
        WARNING,
        ERROR
    }
}
