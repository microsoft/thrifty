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
package com.microsoft.example.processor

import com.microsoft.thrifty.compiler.spi.KotlinTypeProcessor
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.Serializable

/**
 * An example [KotlinTypeProcessor] that implements [Serializable]
 * for all generated types.
 */
class ExampleTypeProcessor : KotlinTypeProcessor {
    private var firstRun = true

    override fun process(spec: TypeSpec): TypeSpec? {
        if (firstRun) {
            println("I AM IN A TYPE PROCESSOR")
            firstRun = false
        }

        return spec
    }
}
