package com.bendb.thrifty.transport;

import okio.BufferedSink;
import okio.BufferedSource;

import java.io.Closeable;

public abstract class Transport implements Closeable {
    public abstract BufferedSource source();

    public abstract BufferedSink sink();
}
