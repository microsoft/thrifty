/*
 * Copyright (C) 2015 Benjamin Bader
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
        out.printf("testDouble(%f)", thing);
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
        out.printf("testMap(%s)\n", thing.toString());
        return thing;
    }

    @Override
    public Map<String, String> testStringMap(Map<String, String> thing) throws TException {
        out.printf("testStringMap(%s)\n", thing);
        return thing;
    }

    @Override
    public Set<Integer> testSet(Set<Integer> thing) throws TException {
        return null;
    }

    @Override
    public List<Integer> testList(List<Integer> thing) throws TException {
        return null;
    }

    @Override
    public Numberz testEnum(Numberz thing) throws TException {
        return null;
    }

    @Override
    public long testTypedef(long thing) throws TException {
        return 0;
    }

    @Override
    public Map<Integer, Map<Integer, Integer>> testMapMap(int hello) throws TException {
        return null;
    }

    @Override
    public Map<Long, Map<Numberz, Insanity>> testInsanity(Insanity argument) throws TException {
        return null;
    }

    @Override
    public Xtruct testMulti(byte arg0, int arg1, long arg2, Map<Short, String> arg3, Numberz arg4, long arg5) throws TException {
        return null;
    }

    @Override
    public void testException(String arg) throws Xception, TException {

    }

    @Override
    public Xtruct testMultiException(String arg0, String arg1) throws Xception, Xception2, TException {
        return null;
    }

    @Override
    public void testOneway(int secondsToSleep) throws TException {

    }
}
