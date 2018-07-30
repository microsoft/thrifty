package com.microsoft.thrifty.schema

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.junit.Test
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
        "$location" shouldBe "/var/log${File.separator}syslog at 10:5"
    }
}