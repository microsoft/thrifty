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

@AutoValue
public abstract class ThriftFileElement {
    public abstract Location location();
    public abstract ImmutableList<NamespaceElement> namespaces();
    public abstract ImmutableList<IncludeElement> includes();
    public abstract ImmutableList<ConstElement> constants();
    public abstract ImmutableList<TypedefElement> typedefs();
    public abstract ImmutableList<EnumElement> enums();
    public abstract ImmutableList<StructElement> structs();
    public abstract ImmutableList<StructElement> unions();
    public abstract ImmutableList<StructElement> exceptions();
    public abstract ImmutableList<ServiceElement> services();

    public static Builder builder(Location location) {
        return new AutoValue_ThriftFileElement.Builder()
                .location(location)
                .namespaces(ImmutableList.of())
                .includes(ImmutableList.of())
                .constants(ImmutableList.of())
                .typedefs(ImmutableList.of())
                .enums(ImmutableList.of())
                .structs(ImmutableList.of())
                .unions(ImmutableList.of())
                .exceptions(ImmutableList.of())
                .services(ImmutableList.of());
    }

    ThriftFileElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder namespaces(List<NamespaceElement> namespaces);
        Builder includes(List<IncludeElement> includes);
        Builder constants(List<ConstElement> constants);
        Builder typedefs(List<TypedefElement> typedefs);
        Builder enums(List<EnumElement> enums);
        Builder structs(List<StructElement> structs);
        Builder unions(List<StructElement> unions);
        Builder exceptions(List<StructElement> exceptions);
        Builder services(List<ServiceElement> services);

        ThriftFileElement build();
    }
}
