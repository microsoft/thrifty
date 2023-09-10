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

internal object SortUtil {
    fun <T> inDependencyOrder(nodes: List<T>, getRefs: (T) -> List<T>): List<T> {
        // KAAAAAAAAAAAAAAHHHHHHNNNNNNNNNNNN!!!!!!!!! ('s algorithm)

        // A mapping of nodes to the number of other nodes referencing them
        val references = LinkedHashMap<T, Int>()
        val roots = ArrayDeque<T>()

        // ArrayDeque implements List and allows efficient insertion at the head,
        // allowing us to avoid a '.reversed()' call.
        val result = ArrayDeque<T>()

        for (n in nodes) {
            // We need to ensure that every node has a refcount
            references.computeIfAbsent(n) { 0 }

            for (r in getRefs(n)) {
                // Every node r is referenced by the current node n, so
                // we can increment its refcount
                references.compute(r) { _, maybeCount -> (maybeCount ?: 0) + 1 }
            }
        }

        for ((n, numRefs) in references) {
            if (numRefs == 0) {
                roots.add(n)
            }
        }

        while (roots.isNotEmpty()) {
            val n = roots.removeFirst()
            result.addFirst(n)

            for (r in getRefs(n)) {
                val numRefs = references.compute(r) { _, numRefs -> numRefs!! - 1 }
                if (numRefs == 0) {
                    roots.add(r)
                }
            }
        }

        return result
    }
}
