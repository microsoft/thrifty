package com.bendb.thrifty;

import com.bendb.thrifty.protocol.Protocol;

import java.io.IOException;

/**
 * An object that can read and write a Thrift struct of type {@link T} from and
 * to a {@link Protocol} object.
 * @param <T> the type of struct that can be written and read
 * @param <B> a {@link StructBuilder} for {@link T}.
 */
public interface Adapter<T, B extends StructBuilder<T>> {
    /**
     * Reads a new instane of {@link T} from the given {@code protocol}.
     *
     * @param protocol the protocol from which to read
     * @return an instance of {@link T} populated with the data just read.
     * @throws IOException if reading fails, or if the struct is malformed.
     */
    T read(Protocol protocol) throws IOException;

    /**
     * Reads a new instane of {@link T} from the given {@code protocol}, using
     * the pre-allocated {@code builder}.
     *
     * @param protocol the protocol from which to read
     * @param builder a builder for {@link T}
     * @return an instance of {@link T} populated with the data just read.
     * @throws IOException if reading fails, or if the struct is malformed.
     */
    T read(Protocol protocol, B builder) throws IOException;

    /**
     * Writes the given {@code struct} to the given {@code protocol}.
     *
     * @param protocol the protocol to which to write the struct
     * @param struct the struct to be written
     * @throws IOException if writing fails
     */
    void write(Protocol protocol, T struct) throws IOException;
}
