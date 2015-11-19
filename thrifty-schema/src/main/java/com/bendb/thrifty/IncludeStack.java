package com.bendb.thrifty;

import com.google.common.collect.AbstractIterator;

import java.util.Iterator;

abstract class IncludeStack implements Iterable<String> {

    public static IncludeStack empty() {
        return Empty.INSTANCE;
    }

    public static IncludeStack of(String value) {
        return empty().push(value);
    }

    public static IncludeStack of(Iterable<String> values) {
        IncludeStack stack = empty();
        for (String value : values) {
            stack = stack.push(value);
        }
        return stack;
    }

    abstract boolean isEmpty();
    abstract String getValue();
    abstract IncludeStack pop();

    IncludeStack push(String value) {
        return new NonEmpty(this, value);
    }

    @Override
    public Iterator<String> iterator() {
        return new StackIterator(this);
    }

    private static class Empty extends IncludeStack {
        private static final Empty INSTANCE = new Empty();

        private Empty() {
            // no instances
        }

        @Override
        boolean isEmpty() {
            return true;
        }

        @Override
        String getValue() {
            throw new UnsupportedOperationException("empty stack");
        }

        @Override
        IncludeStack pop() {
            throw new UnsupportedOperationException("empty stack");
        }
    }

    private static class NonEmpty extends IncludeStack {
        private final IncludeStack parent;
        private final String value;

        NonEmpty(IncludeStack parent, String value) {
            this.parent = parent;
            this.value = value;
        }

        @Override
        boolean isEmpty() {
            return false;
        }

        @Override
        String getValue() {
            return value;
        }

        @Override
        IncludeStack pop() {
            return parent;
        }
    }

    private static class StackIterator extends AbstractIterator<String> {
        private IncludeStack stack;

        StackIterator(IncludeStack stack) {
            this.stack = stack;
        }

        @Override
        protected String computeNext() {
            if (!stack.isEmpty()) {
                String value = stack.getValue();
                stack = stack.pop();
                return value;
            } else {
                return endOfData();
            }
        }
    }
}
