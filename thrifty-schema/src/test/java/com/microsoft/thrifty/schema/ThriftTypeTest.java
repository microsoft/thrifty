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
package com.microsoft.thrifty.schema;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ThriftTypeTest {
    @Test
    public void byteAndI8AreSynonyms() {
        final AtomicInteger ctr = new AtomicInteger(0);

        ThriftType.Visitor<Void> v = new ThriftType.Visitor<Void>() {
            @Override
            public Void visitVoid(BuiltinType voidType) {
                return null;
            }

            @Override
            public Void visitBool(BuiltinType boolType) {
                return null;
            }

            @Override
            public Void visitByte(BuiltinType byteType) {
                ctr.incrementAndGet();
                return null;
            }

            @Override
            public Void visitI16(BuiltinType i16Type) {
                return null;
            }

            @Override
            public Void visitI32(BuiltinType i32Type) {
                return null;
            }

            @Override
            public Void visitI64(BuiltinType i64Type) {
                return null;
            }

            @Override
            public Void visitDouble(BuiltinType doubleType) {
                return null;
            }

            @Override
            public Void visitString(BuiltinType stringType) {
                return null;
            }

            @Override
            public Void visitBinary(BuiltinType binaryType) {
                return null;
            }

            @Override
            public Void visitEnum(EnumType enumType) {
                return null;
            }

            @Override
            public Void visitList(ListType listType) {
                return null;
            }

            @Override
            public Void visitSet(SetType setType) {
                return null;
            }

            @Override
            public Void visitMap(MapType mapType) {
                return null;
            }

            @Override
            public Void visitStruct(StructType structType) {
                return null;
            }

            @Override
            public Void visitTypedef(TypedefType typedefType) {
                return null;
            }

            @Override
            public Void visitService(ServiceType serviceType) {
                return null;
            }
        };

        BuiltinType.I8.accept(v);
        BuiltinType.BYTE.accept(v);

        assertThat(BuiltinType.I8.isBuiltin(), is(true));
        assertThat(ctr.get(), is(2));
    }

    @Test
    public void typesWithSameNameAreEqual() {
        ThriftType one = BuiltinType.get("i32");
        ThriftType two = BuiltinType.get("i32");

        assertThat(one, equalTo(two));
    }

    @Test
    public void annotationsDoNotAffectEquality() {
        ThriftType one = BuiltinType.get("i32").withAnnotations(Collections.singletonMap("test", "one"));
        ThriftType two = BuiltinType.get("i32").withAnnotations(Collections.singletonMap("test", "two"));

        assertThat(one, equalTo(two));
    }

    @Test
    public void withAnnotationsMergesAnnotations() {
        ThriftType one = BuiltinType.get("i32").withAnnotations(Collections.singletonMap("i32", "bar"));
        ThriftType two = one.withAnnotations(Collections.singletonMap("baz", "quux"));

        assertThat(two.annotations(), hasEntry("i32", "bar"));
        assertThat(two.annotations(), hasEntry("baz", "quux"));
    }

    @Test
    public void typeAnnotationsAreImmutable() {
        ThriftType one = BuiltinType.get("i32").withAnnotations(Collections.singletonMap("i32", "bar"));
        try {
            //noinspection deprecation
            one.annotations().put("baz", "quux");
            fail("Expected ThriftType#annotations() to be immutable!");
        } catch (UnsupportedOperationException ignored) {
            // pass
        }
    }
}