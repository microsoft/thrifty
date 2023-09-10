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
package com.bendb.thrifty.compiler

import com.bendb.thrifty.compiler.spi.KotlinTypeProcessor
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.Serializable

/**
 * An example [KotlinTypeProcessor] that implements [Serializable]
 * for all generated types.
 */
class SerializableKotlinProcessor : KotlinTypeProcessor {
    override fun process(spec: TypeSpec): TypeSpec? {
        return spec.toBuilder().run {
            addSuperinterface(Serializable::class)

            // Static fields in Kotlin go in a companion object;
            // we'll assume here that `spec` does not already
            // have one.
            val companionType = TypeSpec.companionObjectBuilder()
                    .addProperty(PropertySpec.builder("serialVersionUID", Long::class)
                            .addModifiers(KModifier.PRIVATE, KModifier.CONST) // const vals in companions are static
                            .initializer("%L", -1L)
                            .build())
                    .build()

            addType(companionType)
            build()
        }
    }
}
