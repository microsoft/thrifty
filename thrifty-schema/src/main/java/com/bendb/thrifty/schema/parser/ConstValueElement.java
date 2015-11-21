package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
        SET,
    }

    public abstract Location location();
    public abstract Kind kind();
    public abstract Object value();

    ConstValueElement() {}

    public static ConstValueElement integer(Location location, int value) {
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
