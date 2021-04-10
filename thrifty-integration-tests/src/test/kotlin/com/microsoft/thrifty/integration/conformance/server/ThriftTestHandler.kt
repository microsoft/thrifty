package com.microsoft.thrifty.integration.conformance.server

import com.microsoft.thrifty.integration.kgen.coro.ThriftTest
import com.microsoft.thrifty.integration.kgen.coro.UserId
import okio.ByteString
import org.apache.thrift.TException
import java.util.LinkedHashMap

class ThriftTestHandler : ThriftTest {
    override suspend fun testVoid() {

    }

    override suspend fun testString(thing: String): String {
        return thing
    }

    override suspend fun testBool(thing: Boolean): Boolean {
        return thing
    }

    override suspend fun testByte(thing: Byte): Byte {
        return thing
    }

    override suspend fun testI32(thing: Int): Int {
        return thing
    }

    override suspend fun testI64(thing: Long): Long {
        return thing
    }

    override suspend fun testDouble(thing: Double): Double {
        return thing
    }

    override suspend fun testBinary(thing: ByteString): ByteString {
        return thing
    }

    override suspend fun testStruct(thing: com.microsoft.thrifty.integration.kgen.coro.Xtruct): com.microsoft.thrifty.integration.kgen.coro.Xtruct {
        return thing
    }

    override suspend fun testNest(thing: com.microsoft.thrifty.integration.kgen.coro.Xtruct2): com.microsoft.thrifty.integration.kgen.coro.Xtruct2 {
        return thing
    }

    override suspend fun testMap(thing: Map<Int, Int>): Map<Int, Int> {
        return thing
    }

    override suspend fun testStringMap(thing: Map<String, String>): Map<String, String> {
        return thing
    }

    override suspend fun testSet(thing: Set<Int>): Set<Int> {
        return thing
    }

    override suspend fun testList(thing: List<Int>): List<Int> {
        return thing
    }

    override suspend fun testEnum(thing: com.microsoft.thrifty.integration.kgen.coro.Numberz): com.microsoft.thrifty.integration.kgen.coro.Numberz {
        return thing
    }

    override suspend fun testTypedef(thing: UserId): UserId {
        return thing
    }

    override suspend fun testMapMap(hello: Int): Map<Int, Map<Int, Int>> {
        // {-4 => {-4 => -4, -3 => -3, -2 => -2, -1 => -1, }, 4 => {1 => 1, 2 => 2, 3 => 3, 4 => 4, }, }

        // {-4 => {-4 => -4, -3 => -3, -2 => -2, -1 => -1, }, 4 => {1 => 1, 2 => 2, 3 => 3, 4 => 4, }, }
        val result: MutableMap<Int, Map<Int, Int>> = LinkedHashMap()
        val first: MutableMap<Int, Int> = LinkedHashMap()
        val second: MutableMap<Int, Int> = LinkedHashMap()

        first[-4] = -4
        first[-3] = -3
        first[-2] = -2
        first[-1] = -1

        second[1] = 1
        second[2] = 2
        second[3] = 3
        second[4] = 4

        result[-4] = first
        result[4] = second

        return result
    }

    override suspend fun testInsanity(argument: com.microsoft.thrifty.integration.kgen.coro.Insanity): Map<UserId, Map<com.microsoft.thrifty.integration.kgen.coro.Numberz, com.microsoft.thrifty.integration.kgen.coro.Insanity>> {
        /*
     *   { 1 => { 2 => argument,
     *            3 => argument,
     *          },
     *     2 => { 6 => <empty Insanity struct>, },
     *   }
     */


        /*
     *   { 1 => { 2 => argument,
     *            3 => argument,
     *          },
     *     2 => { 6 => <empty Insanity struct>, },
     *   }
     */
        val result: MutableMap<Long, Map<com.microsoft.thrifty.integration.kgen.coro.Numberz, com.microsoft.thrifty.integration.kgen.coro.Insanity>> = LinkedHashMap()
        val first: MutableMap<com.microsoft.thrifty.integration.kgen.coro.Numberz, com.microsoft.thrifty.integration.kgen.coro.Insanity> = LinkedHashMap()
        val second: MutableMap<com.microsoft.thrifty.integration.kgen.coro.Numberz, com.microsoft.thrifty.integration.kgen.coro.Insanity> = LinkedHashMap()

        first[com.microsoft.thrifty.integration.kgen.coro.Numberz.TWO] = argument
        first[com.microsoft.thrifty.integration.kgen.coro.Numberz.THREE] = argument

        second[com.microsoft.thrifty.integration.kgen.coro.Numberz.SIX] = com.microsoft.thrifty.integration.kgen.coro.Insanity(null, null)

        result[1L] = first
        result[2L] = second

        return result
    }

    override suspend fun testMulti(arg0: Byte, arg1: Int, arg2: Long, arg3: Map<Short, String>, arg4: com.microsoft.thrifty.integration.kgen.coro.Numberz, arg5: UserId): com.microsoft.thrifty.integration.kgen.coro.Xtruct {
        return com.microsoft.thrifty.integration.kgen.coro.Xtruct("Hello2", arg0, arg1, arg2, null, null)
    }

    override suspend fun testException(arg: String) {
        if ("TException" == arg) {
            throw TException()
        } else if ("Xception" == arg) {
            throw com.microsoft.thrifty.integration.kgen.coro.Xception(1001, "Xception")
        }
    }

    override suspend fun testMultiException(arg0: String, arg1: String): com.microsoft.thrifty.integration.kgen.coro.Xtruct {
        if ("Xception" == arg0) {
            throw com.microsoft.thrifty.integration.kgen.coro.Xception(1001, "This is an Xception")
        } else if ("Xception2" == arg0) {
            val xtruct = com.microsoft.thrifty.integration.kgen.coro.Xtruct(
                    string_thing = "This is an Xception2",
                    byte_thing = null,
                    i32_thing = null,
                    i64_thing = null,
                    double_thing = null,
                    bool_thing = null
            )
            throw com.microsoft.thrifty.integration.kgen.coro.Xception2(2002, xtruct)
        }

        return com.microsoft.thrifty.integration.kgen.coro.Xtruct(
                string_thing = arg1,
                byte_thing = null,
                i32_thing = null,
                i64_thing = null,
                double_thing = null,
                bool_thing = null
        )
    }

    override suspend fun testOneway(secondsToSleep: Int) {
    }

    override suspend fun testUnionArgument(arg0: com.microsoft.thrifty.integration.kgen.coro.NonEmptyUnion): com.microsoft.thrifty.integration.kgen.coro.HasUnion {
        val result = com.microsoft.thrifty.integration.kgen.coro.HasUnion(arg0)
        return result
    }

    override suspend fun testUnionWithDefault(theArg: com.microsoft.thrifty.integration.kgen.coro.UnionWithDefault): com.microsoft.thrifty.integration.kgen.coro.UnionWithDefault {
        return theArg
    }

}
