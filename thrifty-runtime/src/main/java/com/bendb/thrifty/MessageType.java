package com.bendb.thrifty;

public final class MessageType {
    public static final byte CALL = 1;
    public static final byte REPLY = 2;
    public static final byte EXCEPTION = 3;
    public static final byte ONEWAY = 4;

    private MessageType() {
        // no instances
    }
}
