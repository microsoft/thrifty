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
import org.gradle.api.tasks.Optional;

import java.io.Serializable;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Thrift options specific to the Kotlin language.
 */
public class KotlinThriftOptions extends ThriftOptions implements Serializable {
    public enum ClientStyle {
        NONE,
        DEFAULT,
        COROUTINE,
    }

    private ClientStyle serviceClientStyle = null;
    private boolean structBuilders = false;

    @Input
    @Optional
    public ClientStyle getServiceClientStyle() {
        return serviceClientStyle;
    }

    public void setServiceClientStyle(String clientStyleName) {
        TreeMap<String, ClientStyle> stylesByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ClientStyle style : ClientStyle.values()) {
            stylesByName.put(style.name(), style);
        }

        ClientStyle clientStyle = stylesByName.get(clientStyleName);
        if (clientStyle == null) {
            StringBuilder sb = new StringBuilder("Invalid client style; allowed values are:\n");
            for (ClientStyle value : stylesByName.values()) {
                sb.append("\t- ");
                sb.append(value.name().toLowerCase(Locale.US));
                sb.append("\n");
            }
            throw new IllegalArgumentException(sb.toString());
        }

        setServiceClientStyle(clientStyle);
    }

    public void setServiceClientStyle(ClientStyle clientStyle) {
        setGenerateServiceClients(clientStyle != ClientStyle.NONE);
        this.serviceClientStyle = clientStyle;
    }

    @Override
    public void setGenerateServiceClients(boolean generateServiceClients) {
        super.setGenerateServiceClients(generateServiceClients);
        if (generateServiceClients) {
            if (serviceClientStyle == ClientStyle.NONE) {
                serviceClientStyle = ClientStyle.DEFAULT;
            }
        } else {
            serviceClientStyle = ClientStyle.NONE;
        }
    }

    @Input
    public boolean getStructBuilders() {
        return structBuilders;
    }

    public void setStructBuilders(boolean structBuilders) {
        this.structBuilders = structBuilders;
    }
}
