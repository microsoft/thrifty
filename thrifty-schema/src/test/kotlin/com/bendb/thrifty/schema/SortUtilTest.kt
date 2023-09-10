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
package com.bendb.thrifty.schema

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import org.junit.jupiter.api.Test

class SortUtilTest {
    class Node(
        val label: String
    ) {
        var refs: MutableList<Node> = mutableListOf()

        override fun equals(other: Any?): Boolean {
            return other !== null && other is Node && label == other.label
        }

        override fun hashCode(): Int {
            return label.hashCode()
        }

        override fun toString(): String {
            return label
        }
    }

    @Test
    fun sortsInDependencyOrder() {
        val nodes = sort("""
            A -> F
            B -> A
            C
            D -> B, C
            E -> C, F
            F
        """.trimIndent())

        nodes should beDependencySorted()
    }

    private fun beDependencySorted(): DependencyOrderMatcher {
        return DependencyOrderMatcher()
    }

    private class DependencyOrderMatcher : Matcher<Collection<Node>> {
        override fun test(value: Collection<Node>): MatcherResult {
            val seen = mutableSetOf<Node>()
            var isSorted = true

            outer@for (node in value) {
                for (ref in node.refs) {
                    if (ref !in seen) {
                        isSorted = false
                        break@outer
                    }
                }

                seen.add(node)
            }

            return MatcherResult(
                passed = isSorted,
                failureMessageFn = { "$value should have been topologically sorted" },
                negatedFailureMessageFn = { "$value should not have been topologically sorted" })
        }
    }

    private fun sort(graph: String): List<Node> {
        val nodes = parseNodes(graph)
        return SortUtil.inDependencyOrder(nodes) { it.refs }
    }

    private fun parseNodes(graph: String): List<Node> {
        val nodes = LinkedHashMap<String, Node>()
        for (line in graph.lineSequence()) {
            if (line.isEmpty()) {
                continue
            }
            val arrowIndex = line.indexOf("->")
            if (arrowIndex == -1) {
                // Line is a node with no edges
                val label = line.trim()
                nodes.computeIfAbsent(label) { Node(label) }
                continue
            }

            val label = line.substring(0, arrowIndex).trim()
            val edgesText = line.substring(arrowIndex + "->".length).trim()
            val edgeLabels = edgesText.split(",").map(String::trim)
            val node = nodes.computeIfAbsent(label) { Node(label) }
            for (refLabel in edgeLabels) {
                val ref = nodes.computeIfAbsent(refLabel) { Node(refLabel) }
                node.refs.add(ref)
            }
        }
        return nodes.toList().map { it.second }
    }
}
