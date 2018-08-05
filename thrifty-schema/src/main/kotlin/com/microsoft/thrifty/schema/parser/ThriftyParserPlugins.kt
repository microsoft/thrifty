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
package com.microsoft.thrifty.schema.parser

import java.util.UUID

object ThriftyParserPlugins {
    private val DEFAULT_UUID_PROVIDER: UUIDProvider = object : UUIDProvider {
        override fun call(): UUID = UUID.randomUUID()
    }

    @Volatile
    private var uuidProvider = DEFAULT_UUID_PROVIDER

    /**
     * Prevents changing the plugins.
     */
    @Volatile
    private var lockdown: Boolean = false

    /**
     * Prevents changing the plugins from then on.
     *
     *
     * This allows container-like environments to prevent client messing with plugins.
     */
    fun lockdown() {
        lockdown = true
    }

    /**
     * Returns true if the plugins were locked down.
     *
     * @return true if the plugins were locked down
     */
    fun isLockdown(): Boolean {
        return lockdown
    }

    /**
     * @param uuidProvider the provider to use for generating [UUID]s for elements.
     */
    fun setUUIDProvider(uuidProvider: UUIDProvider) {
        if (lockdown) {
            throw IllegalStateException("Plugins can't be changed anymore")
        }
        ThriftyParserPlugins.uuidProvider = uuidProvider
    }

    /**
     * @return a [UUID] as dictated by [uuidProvider]. Default is random UUIDs.
     */
    fun createUUID(): UUID {
        return uuidProvider.call()
    }

    fun reset() {
        uuidProvider = DEFAULT_UUID_PROVIDER
    }

    /**
     * A simple provider interface for creating [UUID]s.
     */
    interface UUIDProvider {

        /**
         * @return a [UUID].
         */
        fun call(): UUID
    }
}