package com.bendb.thrifty.parser;

import com.bendb.thrifty.Location;
import com.bendb.thrifty.NamespaceScope;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ThriftParserTest {
    @Test
    public void namespaces() {
        String thrift =
                "namespace java com.bendb.thrifty.parser\n" +
                "namespace cpp bendb.thrifty\n" +
                "namespace * bendb.thrifty\n" +
                "php_namespace 'single_quoted_namespace'\n" +
                "php_namespace \"double_quoted_namespace\"";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "namespaces.thrift"), thrift);

        assertThat(file.namespaces().size(), is(5));
        assertThat(file.namespaces().get(0).scope(), is(NamespaceScope.JAVA));
        assertThat(file.namespaces().get(0).namespace(), is("com.bendb.thrifty.parser"));

        assertThat(file.namespaces().get(1).scope(), is(NamespaceScope.CPP));
        assertThat(file.namespaces().get(1).namespace(), is("bendb.thrifty"));

        assertThat(file.namespaces().get(2).scope(), is(NamespaceScope.ALL));
        assertThat(file.namespaces().get(2).namespace(), is("bendb.thrifty"));

        assertThat(file.namespaces().get(3).scope(), is(NamespaceScope.PHP));
        assertThat(file.namespaces().get(3).namespace(), is("single_quoted_namespace"));

        assertThat(file.namespaces().get(4).scope(), is(NamespaceScope.PHP));
        assertThat(file.namespaces().get(4).namespace(), is("double_quoted_namespace"));
    }

    @Test
    public void phpScopedNamespaceFails() {
        String thrift = "namespace php should.not.work";
        try {
            ThriftParser.parse(Location.get("", "test.php"), thrift);
            fail("Scoped namespace statements for PHP should not pass parsing");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("not supported"));
        }
    }

    @Test
    public void includes() {
        String thrift =
                "include 'inc/common.thrift'\n" +
                "include \".././parent.thrift\"\n" +
                "cpp_include 'inc/boost.hpp'\n" +
                "\n" +
                "namespace * bendb";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "includes.thrift"), thrift);

        assertThat(file.includes().size(), is(3));
        assertThat(file.namespaces().size(), is(1));

        assertThat(file.includes().get(0).path(), is("inc/common.thrift"));
        assertThat(file.includes().get(0).isCpp(), is(false));

        assertThat(file.includes().get(1).path(), is(".././parent.thrift"));
        assertThat(file.includes().get(1).isCpp(), is(false));

        assertThat(file.includes().get(2).path(), is("inc/boost.hpp"));
        assertThat(file.includes().get(2).isCpp(), is(true));
    }

    @Test
    public void simpleTypedefs() {
        String thrift =
                "typedef i32 MyInt\n" +
                "typedef string MyString\n" +
                "typedef binary PrivateKey\n";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "typedefs.thrift"), thrift);

        assertThat(file.typedefs().get(0).oldName(), is("i32"));
        assertThat(file.typedefs().get(0).newName(), is("MyInt"));

        assertThat(file.typedefs().get(1).oldName(), is("string"));
        assertThat(file.typedefs().get(1).newName(), is("MyString"));

        assertThat(file.typedefs().get(2).oldName(), is("binary"));
        assertThat(file.typedefs().get(2).newName(), is("PrivateKey"));
    }

    @Test
    public void containerTypedefs() {
        String thrift =
                "typedef list<i32> IntList\n" +
                "typedef set<string> Names\n" +
                "typedef map < i16,set<binary > > BlobMap\n";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "typedefs.thrift"), thrift);

        assertThat(file.typedefs().get(0).oldName(), is("list<i32>"));
        assertThat(file.typedefs().get(0).newName(), is("IntList"));

        assertThat(file.typedefs().get(1).oldName(), is("set<string>"));
        assertThat(file.typedefs().get(1).newName(), is("Names"));

        assertThat(file.typedefs().get(2).oldName(), is("map<i16, set<binary>>"));
        assertThat(file.typedefs().get(2).newName(), is("BlobMap"));
    }

    @Test
    public void emptyStruct() {
        String thrift = "struct Empty {}";
        ThriftFileElement file = ThriftParser.parse(Location.get("", "empty.thrift"), thrift);

        assertThat(file.structs().get(0).name(), is("Empty"));
        assertThat(file.structs().get(0).type(), is(StructElement.Type.STRUCT));
        assertThat(file.structs().get(0).fields().size(), is(0));
    }

    @Test
    public void simpleStruct() {
        String thrift = "struct Simple {\n" +
                "  /** This field is optional */\n" +
                "  1:i32 foo,\n" +
                "  // This next field is required\n" +
                "  2: required string bar\n" +
                "}";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "simple.thrift"), thrift);
        assertThat(file.structs().get(0).name(), is("Simple"));
        assertThat(file.structs().get(0).type(), is(StructElement.Type.STRUCT));

        FieldElement first = file.structs().get(0).fields().get(0);
        assertThat(first.documentation(), is("This field is optional"));
        assertThat(first.fieldId(), is(1));
        assertThat(first.type(), is("i32"));
        assertThat(first.required(), is(false));
        assertThat(first.name(), is("foo"));

        FieldElement second = file.structs().get(0).fields().get(1);
        assertThat(second.fieldId(), is(2));
        assertThat(second.documentation(), is("This next field is required"));
        assertThat(second.type(), is("string"));
        assertThat(second.required(), is(true));
        assertThat(second.name(), is("bar"));
    }
}
