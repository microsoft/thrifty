/*
 * Copyright (C) 2015 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
