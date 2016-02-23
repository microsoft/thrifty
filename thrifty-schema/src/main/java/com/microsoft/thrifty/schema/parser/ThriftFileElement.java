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
                .namespaces(ImmutableList.<NamespaceElement>of())
                .includes(ImmutableList.<IncludeElement>of())
                .constants(ImmutableList.<ConstElement>of())
                .typedefs(ImmutableList.<TypedefElement>of())
                .enums(ImmutableList.<EnumElement>of())
                .structs(ImmutableList.<StructElement>of())
                .unions(ImmutableList.<StructElement>of())
                .exceptions(ImmutableList.<StructElement>of())
                .services(ImmutableList.<ServiceElement>of());
    }

    ThriftFileElement() { }

    @AutoValue.Builder
    public interface Builder {
        Builder location(Location location);
        Builder namespaces(ImmutableList<NamespaceElement> namespaces);
        Builder includes(ImmutableList<IncludeElement> includes);
        Builder constants(ImmutableList<ConstElement> constants);
        Builder typedefs(ImmutableList<TypedefElement> typedefs);
        Builder enums(ImmutableList<EnumElement> enums);
        Builder structs(ImmutableList<StructElement> structs);
        Builder unions(ImmutableList<StructElement> unions);
        Builder exceptions(ImmutableList<StructElement> exceptions);
        Builder services(ImmutableList<ServiceElement> services);

        ThriftFileElement build();
    }
}
