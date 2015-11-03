/*
 * Copyright (C) 2015 Benjamin Bader
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty;

import com.google.common.base.Preconditions;

import java.io.File;

public final class Location {
    private final String base;
    private final String path;
    private final int line;
    private final int column;

    public static Location get(String base, String path) {
        return new Location(base, path, -1, -1);
    }

    private Location(String base, String path, int line, int column) {
        this.base = Preconditions.checkNotNull(base, "base");
        this.path = Preconditions.checkNotNull(path, "path");
        this.line = line;
        this.column = column;

        Preconditions.checkArgument(line > 0 || line == -1, "line: " + line);
        Preconditions.checkArgument(column > 0 || column == -1, "column: " + column);
    }

    public Location at(int line, int column) {
        return new Location(base, path, line, column);
    }

    public String base() {
        return base;
    }

    public String path() {
        return path;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(base.length() + path.length());
        if (base.isEmpty()) {
            sb.append(base).append(File.separator);
        }
        sb.append(path);
        if (line != -1) {
            sb.append(" at ").append(line);
            if (column != -1) {
                sb.append(":").append(column);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Location) {
            Location location = (Location) o;

            if (line != location.line) return false;
            if (column != location.column) return false;
            if (!base.equals(location.base)) return false;
            return path.equals(location.path);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = base.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + line;
        result = 31 * result + column;
        return result;
    }
}
