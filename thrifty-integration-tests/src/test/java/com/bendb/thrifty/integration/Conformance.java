package com.bendb.thrifty.integration;

import com.bendb.thrifty.testing.TestServer;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

public class Conformance {
    @Rule public TestServer testServer = new TestServer();

    @Test
    @Ignore
    public void test() {
        fail("Service clients are not yet implemented");
    }
}
