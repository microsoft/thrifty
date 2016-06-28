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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.Location;

import java.util.List;
import java.util.Map;

@AutoValue
public abstract class ConstValueElement {
    public enum Kind {
        INTEGER,
        DOUBLE,
        STRING,
        IDENTIFIER,
        LIST,
        MAP,
    }

    public abstract Location location();
    public abstract Kind kind();
    public abstract Object value();

    public boolean isInt() {
        return kind() == Kind.INTEGER;
    }

    public boolean isDouble() {
        return kind() == Kind.DOUBLE;
    }

    public boolean isString() {
        return kind() == Kind.STRING;
    }

    public boolean isIdentifier() {
        return kind() == Kind.IDENTIFIER;
    }

    public boolean isList() {
        return kind() == Kind.LIST;
    }

    public boolean isMap() {
        return kind() == Kind.MAP;
    }

    public String getAsString() {
        if (kind() == Kind.STRING || kind() == Kind.IDENTIFIER) {
            return (String) value();
        } else {
            throw new IllegalStateException("Cannot convert to string, kind=" + kind());
        }
    }

    public long getAsLong() {
        if (kind() == Kind.INTEGER) {
            return (Long) value();
        } else {
            throw new IllegalStateException("Cannot convert to long, kind=" + kind());
        }
    }

    public int getAsInt() {
        if (kind() == Kind.INTEGER) {
            return ((Long) value()).intValue();
        } else {
            throw new IllegalStateException("Cannot convert to long, kind=" + kind());
        }
    }

    public double getAsDouble() {
        if (kind() == Kind.DOUBLE) {
            return (Double) value();
        } else {
            throw new IllegalStateException("Cannot convert to double, kind=" + kind());
        }
    }

    @SuppressWarnings("unchecked")
    public List<ConstValueElement> getAsList() {
        if (kind() == Kind.LIST) {
            return (List<ConstValueElement>) value();
        } else {
            throw new IllegalStateException("Cannot convert to list, kind=" + kind());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<ConstValueElement, ConstValueElement> getAsMap() {
        if (kind() == Kind.MAP) {
            return (Map<ConstValueElement, ConstValueElement>) value();
        } else {
            throw new IllegalStateException("Cannot convert to map, kind=" + kind());
        }
    }

    ConstValueElement() { }

    public static ConstValueElement integer(Location location, long value) {
        return new AutoValue_ConstValueElement(location, Kind.INTEGER, value);
    }

    public static ConstValueElement real(Location location, double value) {
        return new AutoValue_ConstValueElement(location, Kind.DOUBLE, value);
    }

    public static ConstValueElement literal(Location location, String value) {
        return new AutoValue_ConstValueElement(location, Kind.STRING, value);
    }

    public static ConstValueElement identifier(Location location, String value) {
        return new AutoValue_ConstValueElement(location, Kind.IDENTIFIER, value);
    }

    public static ConstValueElement list(Location location, List<ConstValueElement> elements) {
        return new AutoValue_ConstValueElement(location, Kind.LIST, ImmutableList.copyOf(elements));
    }

    public static ConstValueElement map(Location location, Map<ConstValueElement, ConstValueElement> elements) {
        return new AutoValue_ConstValueElement(location, Kind.MAP, ImmutableMap.copyOf(elements));
    }
}
