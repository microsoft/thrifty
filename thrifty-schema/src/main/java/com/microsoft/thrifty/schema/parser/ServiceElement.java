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
package com.microsoft.thrifty.schema.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.Location;

import java.util.List;

import javax.annotation.Nullable;

@AutoValue
public abstract class ServiceElement {
    public abstract Location location();
    public abstract String documentation();
    public abstract String name();
    @Nullable public abstract TypeElement extendsService();
    public abstract ImmutableList<FunctionElement> functions();
    @Nullable public abstract AnnotationElement annotations();

    ServiceElement() { }

    public static Builder builder(Location location) {
        return new AutoValue_ServiceElement.Builder()
                .location(location)
                .documentation("")
                .functions(ImmutableList.of());
    }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder documentation(String documentation);
        Builder name(String name);
        Builder extendsService(TypeElement serviceName);
        Builder functions(List<FunctionElement> functions);
        Builder annotations(AnnotationElement annotations);

        ServiceElement build();
    }
}
