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
package com.microsoft.thrifty.gradle;

import org.gradle.api.tasks.Input;

import java.io.Serializable;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Thrift options specific to the Java language.
 */
public class JavaThriftOptions extends ThriftOptions implements Serializable {
    public enum NullabilityAnnotations {
        NONE("none"),
        ANDROID_SUPPORT("android-support"),
        ANDROIDX("androidx"),
        ;

        private final String label;

        NullabilityAnnotations(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private NullabilityAnnotations nullabilityAnnotations = NullabilityAnnotations.NONE;

    @Input
    public NullabilityAnnotations getNullabilityAnnotations() {
        return nullabilityAnnotations;
    }

    public void setNullabilityAnnotations(String nullabilityAnnotations) {
        TreeMap<String, NullabilityAnnotations> annotationsByLabel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (NullabilityAnnotations anno : NullabilityAnnotations.values()) {
            annotationsByLabel.put(anno.getLabel(), anno);
        }

        NullabilityAnnotations annotations = annotationsByLabel.get(nullabilityAnnotations);
        if (annotations == null) {
            StringBuilder sb = new StringBuilder("Invalid nullability annotations name; valid values are:\n");
            for (String label : annotationsByLabel.keySet()) {
                sb.append("\t- ");
                sb.append(label);
                sb.append("\n");
            }
            throw new IllegalArgumentException(sb.toString());
        }

        setNullabilityAnnotations(annotations);
    }

    public void setNullabilityAnnotations(NullabilityAnnotations nullabilityAnnotations) {
        Objects.requireNonNull(nullabilityAnnotations);
        this.nullabilityAnnotations = nullabilityAnnotations;
    }
}
