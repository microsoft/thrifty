package com.bendb.thrifty.schema;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ThriftTypeTest {
    @Test
    public void byteAndI8AreSynonyms() {
        final AtomicInteger ctr = new AtomicInteger(0);

        ThriftType.Visitor<Void> v = new ThriftType.Visitor<Void>() {
            @Override
            public Void visitBool() {
                return null;
            }

            @Override
            public Void visitByte() {
                ctr.incrementAndGet();
                return null;
            }

            @Override
            public Void visitI16() {
                return null;
            }

            @Override
            public Void visitI32() {
                return null;
            }

            @Override
            public Void visitI64() {
                return null;
            }

            @Override
            public Void visitDouble() {
                return null;
            }

            @Override
            public Void visitString() {
                return null;
            }

            @Override
            public Void visitBinary() {
                return null;
            }

            @Override
            public Void visitVoid() {
                return null;
            }

            @Override
            public Void visitEnum(ThriftType userType) {
                return null;
            }

            @Override
            public Void visitList(ThriftType.ListType listType) {
                return null;
            }

            @Override
            public Void visitSet(ThriftType.SetType setType) {
                return null;
            }

            @Override
            public Void visitMap(ThriftType.MapType mapType) {
                return null;
            }

            @Override
            public Void visitUserType(ThriftType userType) {
                return null;
            }

            @Override
            public Void visitTypedef(ThriftType.TypedefType typedefType) {
                return null;
            }
        };

        ThriftType.I8.accept(v);
        ThriftType.BYTE.accept(v);

        assertThat(ThriftType.I8.isBuiltin(), is(true));
        assertThat(ctr.get(), is(2));
    }
}