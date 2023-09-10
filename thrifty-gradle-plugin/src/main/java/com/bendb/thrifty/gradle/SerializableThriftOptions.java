/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.gradle;

import com.bendb.thrifty.gradle.JavaThriftOptions.NullabilityAnnotations;
import com.bendb.thrifty.gradle.KotlinThriftOptions.ClientStyle;

import java.io.Serializable;

// Can't just use ThriftOptions cuz Gradle decorates them with non-serializable types,
// and we need to pass these options to Worker API params that must be serializable.
class SerializableThriftOptions implements Serializable {
    static class Kotlin implements Serializable {
        private ClientStyle serviceClientStyle;
        private boolean structBuilders;
        private boolean generateServer;

        // Required for Serializable
        Kotlin() {}

        public Kotlin(ClientStyle serviceClientStyle, boolean structBuilders, boolean generateServer) {
            this.serviceClientStyle = serviceClientStyle;
            this.structBuilders = structBuilders;
            this.generateServer = generateServer;
        }

        public ClientStyle getServiceClientStyle() {
            return serviceClientStyle;
        }

        public boolean isStructBuilders() {
            return structBuilders;
        }

        public boolean isGenerateServer() {
            return generateServer;
        }
    }

    static class Java implements Serializable {
        private NullabilityAnnotations nullabilityAnnotations;

        // Required for Serializable
        Java() {}

        public Java(NullabilityAnnotations nullabilityAnnotations) {
            this.nullabilityAnnotations = nullabilityAnnotations;
        }

        public NullabilityAnnotations getNullabilityAnnotations() {
            return nullabilityAnnotations;
        }
    }

    private boolean generateServiceClients = true;
    private FieldNameStyle nameStyle = FieldNameStyle.DEFAULT;
    private String listType = null;
    private String setType = null;
    private String mapType = null;
    private boolean parcelable = false;
    private boolean allowUnknownEnumValues = false;
    private Kotlin kotlinOpts;
    private Java javaOpts;

    // For Serializable
    SerializableThriftOptions() {}

    SerializableThriftOptions(ThriftOptions options) {
        this.generateServiceClients = options.getGenerateServiceClients();
        this.nameStyle = options.getNameStyle();
        this.listType = options.getListType();
        this.setType = options.getSetType();
        this.mapType = options.getMapType();
        this.parcelable = options.getParcelable();
        this.allowUnknownEnumValues = options.getAllowUnknownEnumValues();

        if (options instanceof KotlinThriftOptions) {
            KotlinThriftOptions kto = (KotlinThriftOptions) options;
            this.kotlinOpts = new Kotlin(kto.getServiceClientStyle(), kto.getStructBuilders(), kto.isGenerateServer());
        } else if (options instanceof JavaThriftOptions) {
            JavaThriftOptions jto = (JavaThriftOptions) options;
            this.javaOpts = new Java(jto.getNullabilityAnnotations());
        } else {
            throw new IllegalArgumentException("Unexpected thrift-options type:" + options);
        }
    }

    public boolean isGenerateServiceClients() {
        return generateServiceClients;
    }

    public FieldNameStyle getNameStyle() {
        return nameStyle;
    }

    public String getListType() {
        return listType;
    }

    public String getSetType() {
        return setType;
    }

    public String getMapType() {
        return mapType;
    }

    public boolean isParcelable() {
        return parcelable;
    }

    public boolean isAllowUnknownEnumValues() {
        return allowUnknownEnumValues;
    }

    public Kotlin getKotlinOpts() {
        return kotlinOpts;
    }

    public Java getJavaOpts() {
        return javaOpts;
    }

    public boolean isJava() {
        return javaOpts != null;
    }

    public boolean isKotlin() {
        return kotlinOpts != null;
    }
}
