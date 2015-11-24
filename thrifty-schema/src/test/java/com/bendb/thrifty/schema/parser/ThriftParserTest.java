package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.bendb.thrifty.schema.NamespaceScope;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;
import java.util.Map;

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

        ThriftFileElement file = ThriftParser.parse(Location.get("", "containerTypedefs.thrift"), thrift);

        TypedefElement typedef = file.typedefs().get(0);
        assertThat(typedef.oldName(), is("list<i32>"));
        assertThat(typedef.newName(), is("IntList"));

        typedef = file.typedefs().get(1);
        assertThat(typedef.oldName(), is("set<string>"));
        assertThat(typedef.newName(), is("Names"));

        typedef = file.typedefs().get(2);
        assertThat(typedef.oldName(), is("map<i16, set<binary>>"));
        assertThat(typedef.newName(), is("BlobMap"));
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
                "  2: required string bar   // and has trailing doc\n" +
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
        assertThat(second.documentation(), is("This next field is required\nand has trailing doc\n"));
        assertThat(second.type(), is("string"));
        assertThat(second.required(), is(true));
        assertThat(second.name(), is("bar"));
    }

    @Test
    public void trailingFieldDoc() {
        String thrift = "// This struct demonstrates trailing comments\n" +
                "struct TrailingDoc {" +
                "  1: required string standard, // cpp-style\n" +
                "  2: required string python,    # py-style\n" +
                "  3: optional binary knr;      /** K&R-style **/\n" +
                "}";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "trailing.thrift"), thrift);
        StructElement doc = file.structs().get(0);

        assertThat(doc.fields().get(0).documentation(), is("\ncpp-style\n"));
        assertThat(doc.fields().get(1).documentation(), is("\npy-style\n"));
        assertThat(doc.fields().get(2).documentation(), is("\n* K&R-style *\n"));
    }

    @Test
    public void duplicateFieldIds() {
        String thrift = "struct Nope {\n" +
                "1: string foo;\n" +
                "1: string bar;\n" +
                "}";

        try {
            ThriftParser.parse(Location.get("", "duplicateIds.thrift"), thrift);
            fail("Structs with duplicate field IDs should fail to parse");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("duplicate field ID:"));
        }
    }

    @Test
    public void implicitDuplicateFieldIds() {
        String thrift = "struct StillNope {\n" +
                "string foo;\n" +
                "string bar;\n" +
                "1: bytes baz\n" +
                "}";

        try {
            ThriftParser.parse(
                    Location.get("", "duplicateImplicitIds.thrift"),
                    thrift);
            fail("Structs with duplicate implicit field IDs should fail to parse");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("duplicate field ID: 1"));
        }
    }

    @Test
    public void weirdFieldPermutations() {
        String thrift = "struct WeirdButLegal {\n" +
                "byte minimal\n" +
                "byte minimalWithSeparator,\n" +
                "byte minimalWithOtherSeparator,\n" +
                "required byte required\n" +
                "required byte requiredWithComma,\n" +
                "required byte requiredWithSemicolon;\n" +
                "optional i16 optional\n" +
                "optional i16 optionalWithComma,\n" +
                "optional i16 optionalWithSemicolon;\n" +
                "10: i32 implicitOptional\n" +
                "11: i32 implicitOptionalWithComma,\n" +
                "12: i32 implicitOptionalWithSemicolon;\n" +
                "13: required i64 requiredId\n" +
                "14: required i64 requiredIdWithComma,\n" +
                "15: required i64 requiredIdWithSemicolon;\n" +
                "}";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "weird.thrift"), thrift);

        StructElement struct = file.structs().get(0);
        ImmutableList<FieldElement> fields = struct.fields();

        assertThat(fields.size(), is(15));

        FieldElement field = fields.get(0);
        assertThat(field.name(), is("minimal"));
        assertThat(field.type(), is("byte"));
        assertThat(field.fieldId(), is(1));

        field = fields.get(1);
        assertThat(field.name(), is("minimalWithSeparator"));
        assertThat(field.type(), is("byte"));
        assertThat(field.fieldId(), is(2));

        field = fields.get(8);
        assertThat(field.name(), is("optionalWithSemicolon"));
        assertThat(field.type(), is("i16"));
        assertThat(field.required(), is(false));
        assertThat(field.fieldId(), is(9));

        field = fields.get(14);
        assertThat(field.name(), is("requiredIdWithSemicolon"));
        assertThat(field.type(), is("i64"));
        assertThat(field.required(), is(true));
        assertThat(field.fieldId(), is(15));
    }

    @Test
    public void invalidFieldIds() {
        String thrift = "struct NegativeId { -1: required i32 nope }";
        try {
            ThriftParser.parse(Location.get("", ""), thrift);
            fail("Should not parse a struct with a negative field ID");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("field ID must be greater than zero"));
        }

        thrift = "struct ZeroId {\n" +
                "  0: option i64 stillNope\n" +
                "}";
        try {
            ThriftParser.parse(Location.get("", ""), thrift);
            fail("Should not parse a struct with a zero field ID");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("field ID must be greater than zero"));
        }
    }

    @Test
    public void services() {
        String thrift = "\n" +
                "service Svc {\n" +
                "  FooResult foo(1:FooRequest request, 2: optional FooMeta meta)\n" +
                "  oneway BarResult bar() throws (1:FooException foo, 2:BarException bar)\n" +
                "}";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "simpleService.thrift"), thrift);
        ServiceElement svc = file.services().get(0);

        ImmutableList<FunctionElement> functions = svc.functions();

        FunctionElement f = functions.get(0);
        assertThat(f.name(), is("foo"));
        assertThat(f.returnType(), is("FooResult"));
        assertThat(f.params().get(0).name(), is("request"));
        assertThat(f.params().get(0).type(), is("FooRequest"));
        assertThat(f.params().get(0).fieldId(), is(1));
        assertThat(f.params().get(0).required(), is(true));

        assertThat(f.params().get(1).name(), is("meta"));
        assertThat(f.params().get(1).type(), is("FooMeta"));
        assertThat(f.params().get(1).fieldId(), is(2));
        assertThat(f.params().get(1).required(), is(false));

        f = functions.get(1);
        assertThat(f.name(), is("bar"));
        assertThat(f.returnType(), is("BarResult"));
        assertThat(f.oneWay(), is(true));
        assertThat(f.params().size(), is(0));

        ImmutableList<FieldElement> exns = f.exceptions();
        assertThat(exns.get(0).name(), is("foo"));
        assertThat(exns.get(0).type(), is("FooException"));
        assertThat(exns.get(0).fieldId(), is(1));

        assertThat(exns.get(1).name(), is("bar"));
        assertThat(exns.get(1).type(), is("BarException"));
        assertThat(exns.get(1).fieldId(), is(2));
    }

    @Test
    public void unions() {
        String thrift = "\n" +
                "union Normal {\n" +
                "  2: i16 foo,\n" +
                "  4: i32 bar\n" +
                "}\n";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "union.thrift"), thrift);
        StructElement union = file.unions().get(0);

        assertThat(union.name(), is("Normal"));
        assertThat(union.fields().size(), is(2));

        FieldElement f = union.fields().get(0);
        assertThat(f.fieldId(), is(2));
        assertThat(f.name(), is("foo"));
        assertThat(f.type(), is("i16"));
        assertThat(f.required(), is(false));

        f = union.fields().get(1);
        assertThat(f.fieldId(), is(4));
        assertThat(f.name(), is("bar"));
        assertThat(f.type(), is("i32"));
        assertThat(f.required(), is(false));
    }

    @Test
    public void unionCannotHaveRequiredField() {
        String thrift = "\n" +
                "union Normal {\n" +
                "  3: optional i16 foo,\n" +
                "  5: required i32 bar\n" +
                "}\n";

        try {
            ThriftParser.parse(Location.get("", "unionWithRequired.thrift"), thrift);
            fail("Union cannot have a required field");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("unions cannot have required fields"));
        }
    }

    @Test
    public void unionCannotHaveMultipleDefaultValues() {
        String thrift = "\n" +
                "union Normal {\n" +
                "  3: i16 foo = 1,\n" +
                "  5: i32 bar = 2\n" +
                "}\n";

        try {
            ThriftParser.parse(Location.get("", "unionWithRequired.thrift"), thrift);
            fail("Union cannot have a more than one default value");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("unions can have at most one default value"));
        }
    }

    @Test
    public void unionsCanHaveOneDefaultValue() {
        String thrift = "\n" +
                "union Default {\n" +
                "  1: i16 foo,\n" +
                "  2: i16 bar,\n" +
                "  3: i16 baz = 0x0FFF,\n" +
                "  4: i16 quux\n" +
                "}";

        ThriftFileElement element = ThriftParser.parse(Location.get("", "unionWithDefault.thrift"), thrift);
        StructElement u = element.unions().get(0);

        assertThat(u.name(), is("Default"));
        assertThat(u.fields().size(), is(4));

        ImmutableList<FieldElement> fields = u.fields();

        FieldElement f = fields.get(0);
        assertThat(f.name(), is("foo"));
        assertThat(f.fieldId(), is(1));
        assertThat(f.type(), is("i16"));
        assertThat(f.constValue(), is(nullValue()));

        f = fields.get(1);
        assertThat(f.name(), is("bar"));
        assertThat(f.fieldId(), is(2));
        assertThat(f.type(), is("i16"));
        assertThat(f.constValue(), is(nullValue()));

        f = fields.get(2);
        assertThat(f.name(), is("baz"));
        assertThat(f.fieldId(), is(3));
        assertThat(f.type(), is("i16"));
        assertThat((Long) f.constValue().value(), is(0xFFFL));

        f = fields.get(3);
        assertThat(f.name(), is("quux"));
        assertThat(f.fieldId(), is(4));
        assertThat(f.type(), is("i16"));
        assertThat(f.constValue(), is(nullValue()));
    }

    @Test
    public void simpleConst() {
        String thrift = "const i64 DefaultStatusCode = 200";
        ThriftFileElement file = ThriftParser.parse(Location.get("", "simpleConst.thrift"), thrift);
        ConstElement c = file.constants().get(0);

        assertThat(c.name(), is("DefaultStatusCode"));
        assertThat(c.type(), is("i64"));
        assertThat(c.value().kind(), is(ConstValueElement.Kind.INTEGER));
        assertThat((Long) c.value().value(), is(200L));
    }

    @Test
    public void listConst() {
        String thrift = "const list<string> Names = [\"foo\" \"bar\", \"baz\"; \"quux\"]";
        ThriftFileElement file = ThriftParser.parse(Location.get("", "listConst.thrift"), thrift);
        ConstElement c = file.constants().get(0);

        assertThat(c.name(), is("Names"));
        assertThat(c.type(), is("list<string>"));

        ConstValueElement value = c.value();
        assertThat(value.kind(), is(ConstValueElement.Kind.LIST));

        List<ConstValueElement> list = (List<ConstValueElement>) value.value();
        assertThat(list.size(), is(4));
        assertThat((String) list.get(0).value(), is("foo"));
        assertThat((String) list.get(1).value(), is("bar"));
        assertThat((String) list.get(2).value(), is("baz"));
        assertThat((String) list.get(3).value(), is("quux"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapConst() {
        String thrift = "const map<string, string> Headers = {\n" +
                "  \"foo\": \"bar\",\n" +
                "  \"baz\": \"quux\";\n" +
                "}";

        ThriftFileElement file = ThriftParser.parse(Location.get("", "mapConst.thrift"), thrift);
        ConstElement c = file.constants().get(0);

        assertThat(c.name(), is("Headers"));
        assertThat(c.type(), is("map<string, string>"));

        ConstValueElement value = c.value();
        assertThat(value.kind(), is(ConstValueElement.Kind.MAP));

        Map<ConstValueElement, ConstValueElement> values = (Map<ConstValueElement, ConstValueElement>) value.value();
        assertThat(values.size(), is(2));

        for (Map.Entry<ConstValueElement, ConstValueElement> entry : values.entrySet()) {
            String key = (String) entry.getKey().value();
            if (key.equals("foo")) {
                assertThat((String) entry.getValue().value(), is("bar"));
            } else if (key.equals("baz")) {
                assertThat((String) entry.getValue().value(), is("quux"));
            } else {
                fail("unexpected key value: " + key);
            }
        }
    }

    @Test
    public void structFieldWithConstValue() {
        String thrift = "struct Foo {\n" +
                "  100: i32 num = 1\n" +
                "}";

        ThriftFileElement file = ThriftParser.parse(Location.get("", ""), thrift);
        StructElement s = file.structs().get(0);
        FieldElement f = s.fields().get(0);
        assertThat(f.fieldId(), is(100));
        assertThat(f.name(), is("num"));
        assertThat(f.type(), is("i32"));

        ConstValueElement v = f.constValue();
        assertThat(v, is(notNullValue()));
        assertThat(v.kind(), is(ConstValueElement.Kind.INTEGER));
        assertThat((Long) v.value(), is(1L));
    }
}
