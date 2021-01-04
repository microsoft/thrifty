package com.microsoft.thrifty.kgen

import com.squareup.kotlinpoet.FileSpec
import io.kotest.matchers.compilation.shouldCompile

fun List<FileSpec>.shouldCompile() {
    forEach {
        it.shouldCompile()
    }
}

fun FileSpec.shouldCompile() {
    toString().lineSequence().forEachIndexed { index, s -> println("${(index+1).toString().padStart(4)}: $s") }
    toString().shouldCompile()
}