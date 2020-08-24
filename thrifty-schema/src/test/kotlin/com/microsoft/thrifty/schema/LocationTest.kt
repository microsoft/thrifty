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
package com.microsoft.thrifty.schema

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.io.File

class LocationTest {
    @Test fun `equals uses structural-equality semantics`() {
        val base = Location.get("/var/log", "test.txt").at(1, 1)

        base shouldBe base
        base shouldNotBe base.at(1, 2)
        base shouldNotBe base.at(2, 1)
        base shouldNotBe Location.get("/var/log", "data.dat").at(1, 1)
        base shouldNotBe Location.get("/etc/config", "test.txt").at(1, 1)
    }

    @Test fun `toString is human-readable`() {
        val location = Location.get("/var/log", "syslog").at(10, 5)
        "$location" shouldBe "/var/log${File.separator}syslog: (10, 5)"
    }
}