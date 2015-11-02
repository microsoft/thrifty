package com.bendb.thrifty;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class Comment {
    private final ImmutableList<String> lines;

    private Comment(Builder builder) {
        lines = ImmutableList.of();
    }

    public class Builder {

    }
}
