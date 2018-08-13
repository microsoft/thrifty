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
package com.microsoft.thrifty.compiler;

import com.microsoft.thrifty.compiler.spi.KotlinTypeProcessor;
import com.microsoft.thrifty.compiler.spi.TypeProcessor;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * An object that locate {@link TypeProcessor} and {@link KotlinTypeProcessor}
 * objects from the current classpath.
 *
 * Used by the compiler to detect and run user-provided processors.
 */
public final class TypeProcessorService {
    private static TypeProcessorService instance;

    public static synchronized TypeProcessorService getInstance() {
        if (instance == null) {
            instance = new TypeProcessorService();
        }

        return instance;
    }

    private ServiceLoader<TypeProcessor> serviceLoader = ServiceLoader.load(TypeProcessor.class);
    private ServiceLoader<KotlinTypeProcessor> kotlinProcessorLoader = ServiceLoader.load(KotlinTypeProcessor.class);

    /**
     * Gets the first {@link TypeProcessor} implementation loaded, or
     * {@code null} if none are found.
     *
     * Because service ordering is non-deterministic, only the first instance
     * is returned.  A warning will be printed if more than one are found.
     *
     * @return The first located {@link TypeProcessor}, or {@code null}.
     */
    public TypeProcessor getJavaProcessor() {
        return loadSingleProcessor(serviceLoader.iterator());
    }

    /**
     * Gets the first {@link KotlinTypeProcessor} implementation loaded, or
     * {@code null} if none are found.
     *
     * Because service ordering is non-deterministic, only the first instance
     * is returned.  A warning will be printed if more than one are found.
     *
     * @return The first located {@link KotlinTypeProcessor}, or {@code null}.
     */
    public KotlinTypeProcessor getKotlinProcessor() {
        return loadSingleProcessor(kotlinProcessorLoader.iterator());
    }

    private <T> T loadSingleProcessor(Iterator<T> iter) {
        T processor = null;

        if (iter.hasNext()) {
            processor = iter.next();

            if (iter.hasNext()) {
                System.err.println("Multiple processors found; using "
                        + processor.getClass().getName());
            }
        }

        return processor;
    }
}
