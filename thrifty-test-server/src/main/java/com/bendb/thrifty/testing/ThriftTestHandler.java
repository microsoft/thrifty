/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.testing;

import com.bendb.thrifty.test.gen.Insanity;
import com.bendb.thrifty.test.gen.Numberz;
import com.bendb.thrifty.test.gen.ThriftTest;
import com.bendb.thrifty.test.gen.Xception;
import com.bendb.thrifty.test.gen.Xception2;
import com.bendb.thrifty.test.gen.Xtruct;
import com.bendb.thrifty.test.gen.Xtruct2;
import org.apache.commons.codec.binary.Hex;
import org.apache.thrift.TException;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of the ThriftTest service, whose semantics are defined
 * in the documentation comments of ThriftTest.thrift.
 */
public class ThriftTestHandler implements ThriftTest.Iface {
    private final PrintStream out;

    ThriftTestHandler(PrintStream out) {
        this.out = out;
    }

    @Override
    public void testVoid() throws TException {
        out.println("testVoid()");
    }

    @Override
    public String testString(String thing) throws TException {
        out.printf("testString(\"%s\")\n", thing);
        return thing;
    }

    @Override
    public boolean testBool(boolean thing) throws TException {
        out.printf("testBool(%b)\n", thing);
        return thing;
    }

    @Override
    public byte testByte(byte thing) throws TException {
        out.printf("testByte(%d)\n", thing);
        return thing;
    }

    @Override
    public int testI32(int thing) throws TException {
        out.printf("testI32(%d)\n", thing);
        return thing;
    }

    @Override
    public long testI64(long thing) throws TException {
        out.printf("testI64(%d)\n", thing);
        return thing;
    }

    @Override
    public double testDouble(double thing) throws TException {
        out.printf("testDouble(%f)\n", thing);
        return thing;
    }

    @Override
    public ByteBuffer testBinary(ByteBuffer thing) throws TException {
        int count = thing.remaining();
        byte[] data = new byte[count];
        thing.get(data);

        out.printf("testBinary(\"%s\")\n", Hex.encodeHexString(data));

        return ByteBuffer.wrap(data);
    }

    @Override
    public Xtruct testStruct(Xtruct thing) throws TException {
        out.printf("testStruct(\"%s\")\n", thing.toString());
        return thing;
    }

    @Override
    public Xtruct2 testNest(Xtruct2 thing) throws TException {
        out.printf("testNest(\"%s\")\n", thing.toString());
        return thing;
    }

    @Override
    public Map<Integer, Integer> testMap(Map<Integer, Integer> thing) throws TException {
        out.printf("testMap(\"%s\")\n", thing.toString());
        return thing;
    }

    @Override
    public Map<String, String> testStringMap(Map<String, String> thing) throws TException {
        out.printf("testStringMap(\"%s\")\n", thing);
        return thing;
    }

    @Override
    public Set<Integer> testSet(Set<Integer> thing) throws TException {
        out.printf("testSet(\"%s\")\n", thing);
        return thing;
    }

    @Override
    public List<Integer> testList(List<Integer> thing) throws TException {
        out.printf("testList(\"%s\")\n", thing);
        return thing;
    }

    @Override
    public Numberz testEnum(Numberz thing) throws TException {
        out.printf("testEnum(%d)\n", thing.getValue());
        return thing;
    }

    @Override
    public long testTypedef(long thing) throws TException {
        out.printf("testTypedef(%d)\n", thing);
        return thing;
    }

    @Override
    public Map<Integer, Map<Integer, Integer>> testMapMap(int hello) throws TException {
        out.printf("testMapMap(%d)\n", hello);

        // {-4 => {-4 => -4, -3 => -3, -2 => -2, -1 => -1, }, 4 => {1 => 1, 2 => 2, 3 => 3, 4 => 4, }, }
        Map<Integer, Map<Integer, Integer>> result = new LinkedHashMap<>();
        Map<Integer, Integer> first = new LinkedHashMap<>();
        Map<Integer, Integer> second = new LinkedHashMap<>();

        first.put(-4, -4);
        first.put(-3, -3);
        first.put(-2, -2);
        first.put(-1, -1);

        second.put(1, 1);
        second.put(2, 2);
        second.put(3, 3);
        second.put(4, 4);

        result.put(-4, first);
        result.put(4, second);

        return result;
    }

    @Override
    public Map<Long, Map<Numberz, Insanity>> testInsanity(Insanity argument) throws TException {
        out.printf("testInsanity(\"{%s}\")\n", argument);

        /*
         *   { 1 => { 2 => argument,
         *            3 => argument,
         *          },
         *     2 => { 6 => <empty Insanity struct>, },
         *   }
         */

        Map<Long, Map<Numberz, Insanity>> result = new LinkedHashMap<>();
        Map<Numberz, Insanity> first = new LinkedHashMap<>();
        Map<Numberz, Insanity> second = new LinkedHashMap<>();

        first.put(Numberz.TWO, argument);
        first.put(Numberz.THREE, argument);

        second.put(Numberz.SIX, new Insanity());

        result.put(1L, first);
        result.put(2L, second);

        return result;
    }

    @Override
    public Xtruct testMulti(byte arg0, int arg1, long arg2, Map<Short, String> arg3, Numberz arg4, long arg5) throws TException {
        out.println("testMulti()");
        return new Xtruct("Hello2", arg0, arg1, arg2);
    }

    @Override
    public void testException(String arg) throws Xception, TException {
        out.printf("testException(%s)\n", arg);
        if ("TException".equals(arg)) {
            throw new TException();
        } else if ("Xception".equals(arg)) {
            throw new Xception(1001, "Xception");
        }
    }

    @Override
    public Xtruct testMultiException(String arg0, String arg1) throws Xception, Xception2, TException {
        out.printf("testMultiException(%s, %s)\n", arg0, arg1);

        if ("Xception".equals(arg0)) {
            throw new Xception(1001, "This is an Xception");
        } else if ("Xception2".equals(arg0)) {
            Xtruct xtruct = new Xtruct().setString_thing("This is an Xception2");
            xtruct.unsetByte_thing();
            xtruct.unsetI32_thing();
            xtruct.unsetI64_thing();
            throw new Xception2(2002, xtruct);
        }

        return new Xtruct().setString_thing(arg1);
    }

    @Override
    public void testOneway(int secondsToSleep) throws TException {

    }
}
