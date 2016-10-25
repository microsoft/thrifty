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
package com.microsoft.thrifty.schema;

import com.google.common.base.Preconditions;

import java.util.Map;

@SuppressWarnings("unchecked")
abstract class AbstractUserElementBuilder<
        TElement extends UserElement,
        TBuilder extends AbstractUserElementBuilder<TElement, TBuilder>> {
    UserElementMixin mixin;

    protected AbstractUserElementBuilder(UserElementMixin mixin) {
        this.mixin = mixin;
    }

    public TBuilder name(String name) {
        Preconditions.checkNotNull(name, "name");
        mixin = mixin.toBuilder().name(name).build();
        return (TBuilder) this;
    }

    public TBuilder location(Location location) {
        Preconditions.checkNotNull(location, "location");
        mixin = mixin.toBuilder().location(location).build();
        return (TBuilder) this;
    }

    public TBuilder documentation(String documentation) {
        Preconditions.checkNotNull(documentation, "documentation");
        mixin = mixin.toBuilder().documentation(documentation).build();
        return (TBuilder) this;
    }

    public TBuilder annotations(Map<String, String> annotations) {
        Preconditions.checkNotNull(annotations, "annotations");
        mixin = mixin.toBuilder().annotations(annotations).build();
        return (TBuilder) this;
    }

    abstract TElement build();
}
