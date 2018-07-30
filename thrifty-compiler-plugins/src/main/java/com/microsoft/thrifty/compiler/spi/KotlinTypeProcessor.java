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
package com.microsoft.thrifty.compiler.spi;

import com.squareup.kotlinpoet.TypeSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * When specified as part of code generation, processes all types after they
 * are computed, but before they are written to disk.  This allows you to make
 * arbitrary modifications to types such as implementing your own interfaces,
 * renaming fields, or anything you might wish to do.
 *
 * <p>For example, a processor that implements java.lang.Serializable on all
 * generated types:
 *
 * <pre><code>
 * class SerializableTypeProcessor : KotlinTypeProcessor {
 *   override fun process(spec: TypeSpec): TypeSpec? {
 *     return type.toBuilder().let {
 *       it.addSuperinterface(Serializable::class)
 *       val companion = TypeSpec.companionBuilder()
 *           .addProperty(PropertySpec.builder("serialVersionUID", Long::class)
 *               .addModifiers(KModifier.PRIVATE)
 *               .jvmField()
 *               .initializer("-1")
 *               .build())
 *           .build()
 *       it.addType(companion)
 *       it.build()
 *     }
 *   }
 * }
 * </code></pre>
 */
public interface KotlinTypeProcessor {
    /**
     * Processes and returns a given type.
     *
     * <p>The given {@code type} will have been generated from compiled Thrift
     * files, and will not have been written to disk.  It can be returned
     * unaltered, or a modified copy can be returned.
     *
     * <p>Finally, if {@code null} is returned, then no file will be generated.
     * This can be used to selectively suppress types, e.g. if it is known that
     * it will be unused.
     *
     * @param type a {@link TypeSpec} generated based on Thrift IDL.
     * @return a (possibly modified) {@link TypeSpec} to be written to disk, or null.
     */
    @Nullable
    TypeSpec process(@NotNull TypeSpec type);
}
