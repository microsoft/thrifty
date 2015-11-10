package com.bendb.thrifty;

/**
 * Represents the wire protocol to which generated adapters wil conform.
 *
 * <p/>The default is {@link #BINARY}.
 */
public enum ThriftProtocol {
    JSON_SIMPLE,
    JSON,
    BINARY,
    COMPACT,
    TUPLE
}
