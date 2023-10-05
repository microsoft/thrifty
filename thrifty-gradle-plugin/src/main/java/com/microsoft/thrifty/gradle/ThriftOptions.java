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
 * Thrift options applicable to all supported languages.
 */
public abstract class ThriftOptions implements Serializable {
    private boolean generateServiceClients = true;
    private FieldNameStyle nameStyle = FieldNameStyle.DEFAULT;
    private String listType = null;
    private String setType = null;
    private String mapType = null;
    private boolean parcelable = false;
    private boolean allowUnknownEnumValues = false;

    @Input
    public boolean getGenerateServiceClients() {
        return generateServiceClients;
    }

    public void setGenerateServiceClients(boolean generateServiceClients) {
        this.generateServiceClients = generateServiceClients;
    }

    @Input
    @Optional
    public FieldNameStyle getNameStyle() {
        return nameStyle;
    }

    public void setNameStyle(String styleName) {
        TreeMap<String, FieldNameStyle> styles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (FieldNameStyle style : FieldNameStyle.values()) {
            styles.put(style.name(), style);
        }

        FieldNameStyle style = styles.get(styleName);
        if (style == null) {
            StringBuilder sb = new StringBuilder("Invalid name style; allowed values are:\n");
            for (FieldNameStyle value : FieldNameStyle.values()) {
                sb.append("\t- ");
                sb.append(value.name().toLowerCase(Locale.US));
                sb.append("\n");
            }
            throw new IllegalArgumentException(sb.toString());
        }

        this.nameStyle = style;
    }

    public void setNameStyle(FieldNameStyle style) {
        this.nameStyle = style;
    }

    @Input
    @Optional
    public String getListType() {
        return listType;
    }

    public void setListType(String listType) {
        this.listType = listType;
    }

    @Input
    @Optional
    public String getSetType() {
        return setType;
    }

    public void setSetType(String setType) {
        this.setType = setType;
    }

    @Input
    @Optional
    public String getMapType() {
        return mapType;
    }

    public void setMapType(String mapType) {
        this.mapType = mapType;
    }

    @Input
    public boolean getParcelable() {
        return parcelable;
    }

    public void setParcelable(boolean parcelable) {
        this.parcelable = parcelable;
    }

    @Input
    public boolean getAllowUnknownEnumValues() {
        return allowUnknownEnumValues;
    }

    public void setAllowUnknownEnumValues(boolean allowUnknownEnumValues) {
        this.allowUnknownEnumValues = allowUnknownEnumValues;
    }
}
