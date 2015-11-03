package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.bendb.thrifty.NamespaceScope;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ThriftParserTest {
    @Test
    public void namespaces() {
        String thrift =
                "namespace java com.bendb.thrifty.parser\n" +
                "namespace cpp bendb.thrifty\n" +
                "namespace * bendb.thrifty";

        ThriftFileElement file = ThriftParser.parse(Location.get("test", ""), thrift);

        assertThat(file.namespaces().size(), is(3));
        assertThat(file.namespaces().get(0).scope(), is(NamespaceScope.JAVA));
        assertThat(file.namespaces().get(0).namespace(), is("com.bendb.thrifty.parser"));

        assertThat(file.namespaces().get(1).scope(), is(NamespaceScope.CPP));
        assertThat(file.namespaces().get(1).namespace(), is("bendb.thrifty"));

        assertThat(file.namespaces().get(2).scope(), is(NamespaceScope.ALL));
        assertThat(file.namespaces().get(2).namespace(), is("bendb.thrifty"));
    }

    @Test
    public void phpScopedNamespace() {
        String thrift = "namespace php should.not.work";
        try {
            ThriftParser.parse(Location.get("test.php", "nope"), thrift);
            fail("Scoped namespace statements for PHP should not pass parsing");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), startsWith("Syntax error"));
        }
    }
}
