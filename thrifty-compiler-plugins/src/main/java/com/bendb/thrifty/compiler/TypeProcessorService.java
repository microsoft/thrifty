package com.bendb.thrifty.compiler;

import com.bendb.thrifty.compiler.spi.TypeProcessor;

import java.util.Iterator;
import java.util.ServiceLoader;

public final class TypeProcessorService {
    private static TypeProcessorService instance;

    public static synchronized TypeProcessorService getInstance() {
        if (instance == null) {
            instance = new TypeProcessorService();
        }

        return instance;
    }

    private ServiceLoader<TypeProcessor> serviceLoader = ServiceLoader.load(TypeProcessor.class);

    /**
     * Gets the first {@link TypeProcessor} implementation loaded, or
     * {@code null} if none are found.
     *
     * Because service ordering is non-deterministic, only the first instance
     * is returned.  A warning will be printed if more than one are found.
     *
     * @return The first located {@link TypeProcessor}, or {@code null}.
     */
    public TypeProcessor get() {
        TypeProcessor processor = null;

        Iterator<TypeProcessor> iter = serviceLoader.iterator();
        if (iter.hasNext()) {
            processor = iter.next();

            if (iter.hasNext()) {
                System.err.println("Multiple TypeProcessors found; using "
                        + processor.getClass().getName());
            }
        }

        return processor;
    }
}
