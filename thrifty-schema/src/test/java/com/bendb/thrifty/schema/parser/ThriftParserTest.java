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
package com.bendb.thrifty.schema.parser;

import com.bendb.thrifty.schema.Location;
import com.bendb.thrifty.schema.NamespaceScope;
import com.google.common.collect.ImmutableList;
import okio.Okio;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

        ThriftFileElement file = parse(thrift, Location.get("", "namespaces.thrift"));

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
    public void includes() {
        String thrift =
                "include 'inc/common.thrift'\n" +
                "include \".././parent.thrift\"\n" +
                "cpp_include 'inc/boost.hpp'\n" +
                "\n" +
                "namespace * bendb";

        ThriftFileElement file = parse(thrift, Location.get("", "includes.thrift"));

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

        ThriftFileElement file = parse(thrift, Location.get("", "typedefs.thrift"));

        assertThat(file.typedefs().get(0).oldType().name(), is("i32"));
        assertThat(file.typedefs().get(0).newName(), is("MyInt"));

        assertThat(file.typedefs().get(1).oldType().name(), is("string"));
        assertThat(file.typedefs().get(1).newName(), is("MyString"));

        assertThat(file.typedefs().get(2).oldType().name(), is("binary"));
        assertThat(file.typedefs().get(2).newName(), is("PrivateKey"));
    }

    @Test
    public void containerTypedefs() {
        String thrift =
                "typedef list<i32> IntList\n" +
                "typedef set<string> Names\n" +
                "typedef map < i16,set<binary > > BlobMap\n";

        ThriftFileElement file = parse(thrift, Location.get("", "containerTypedefs.thrift"));

        TypedefElement typedef = file.typedefs().get(0);
        assertThat(typedef.oldType().name(), is("list<i32>"));
        assertThat(typedef.newName(), is("IntList"));

        typedef = file.typedefs().get(1);
        assertThat(typedef.oldType().name(), is("set<string>"));
        assertThat(typedef.newName(), is("Names"));

        typedef = file.typedefs().get(2);
        assertThat(typedef.oldType().name(), is("map<i16, set<binary>>"));
        assertThat(typedef.newName(), is("BlobMap"));
    }

    @Test
    public void emptyStruct() {
        String thrift = "struct Empty {}";
        ThriftFileElement file = parse(thrift, Location.get("", "empty.thrift"));

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

        ThriftFileElement file = parse(thrift, Location.get("", "simple.thrift"));
        assertThat(file.structs().get(0).name(), is("Simple"));
        assertThat(file.structs().get(0).type(), is(StructElement.Type.STRUCT));

        FieldElement first = file.structs().get(0).fields().get(0);
        assertThat(first.documentation(), is("This field is optional\n"));
        assertThat(first.fieldId(), is(1));
        assertThat(first.type().name(), is("i32"));
        assertThat(first.required(), is(false));
        assertThat(first.name(), is("foo"));

        FieldElement second = file.structs().get(0).fields().get(1);
        assertThat(second.fieldId(), is(2));
        assertThat(second.documentation(), is("This next field is required\nand has trailing doc\n"));
        assertThat(second.type().name(), is("string"));
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

        ThriftFileElement file = parse(thrift, Location.get("", "trailing.thrift"));
        StructElement doc = file.structs().get(0);

        assertThat(doc.fields().get(0).documentation(), is("cpp-style\n"));
        assertThat(doc.fields().get(1).documentation(), is("py-style\n"));
        assertThat(doc.fields().get(2).documentation(), is("* K&R-style *\n"));
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

        ThriftFileElement file = parse(thrift, Location.get("", "weird.thrift"));

        StructElement struct = file.structs().get(0);
        ImmutableList<FieldElement> fields = struct.fields();

        assertThat(fields.size(), is(15));

        FieldElement field = fields.get(0);
        assertThat(field.name(), is("minimal"));
        assertThat(field.type().name(), is("byte"));
        assertThat(field.fieldId(), is(1));

        field = fields.get(1);
        assertThat(field.name(), is("minimalWithSeparator"));
        assertThat(field.type().name(), is("byte"));
        assertThat(field.fieldId(), is(2));

        field = fields.get(8);
        assertThat(field.name(), is("optionalWithSemicolon"));
        assertThat(field.type().name(), is("i16"));
        assertThat(field.required(), is(false));
        assertThat(field.fieldId(), is(9));

        field = fields.get(14);
        assertThat(field.name(), is("requiredIdWithSemicolon"));
        assertThat(field.type().name(), is("i64"));
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

        ThriftFileElement file = parse(thrift, Location.get("", "simpleService.thrift"));
        ServiceElement svc = file.services().get(0);

        ImmutableList<FunctionElement> functions = svc.functions();

        FunctionElement f = functions.get(0);
        assertThat(f.name(), is("foo"));
        assertThat(f.returnType().name(), is("FooResult"));
        assertThat(f.params().get(0).name(), is("request"));
        assertThat(f.params().get(0).type().name(), is("FooRequest"));
        assertThat(f.params().get(0).fieldId(), is(1));
        assertThat(f.params().get(0).required(), is(true));

        assertThat(f.params().get(1).name(), is("meta"));
        assertThat(f.params().get(1).type().name(), is("FooMeta"));
        assertThat(f.params().get(1).fieldId(), is(2));
        assertThat(f.params().get(1).required(), is(false));

        f = functions.get(1);
        assertThat(f.name(), is("bar"));
        assertThat(f.returnType().name(), is("BarResult"));
        assertThat(f.oneWay(), is(true));
        assertThat(f.params().size(), is(0));

        ImmutableList<FieldElement> exns = f.exceptions();
        assertThat(exns.get(0).name(), is("foo"));
        assertThat(exns.get(0).type().name(), is("FooException"));
        assertThat(exns.get(0).fieldId(), is(1));

        assertThat(exns.get(1).name(), is("bar"));
        assertThat(exns.get(1).type().name(), is("BarException"));
        assertThat(exns.get(1).fieldId(), is(2));
    }

    @Test
    public void unions() {
        String thrift = "\n" +
                "union Normal {\n" +
                "  2: i16 foo,\n" +
                "  4: i32 bar\n" +
                "}\n";

        ThriftFileElement file = parse(thrift, Location.get("", "union.thrift"));
        StructElement union = file.unions().get(0);

        assertThat(union.name(), is("Normal"));
        assertThat(union.fields().size(), is(2));

        FieldElement f = union.fields().get(0);
        assertThat(f.fieldId(), is(2));
        assertThat(f.name(), is("foo"));
        assertThat(f.type().name(), is("i16"));
        assertThat(f.required(), is(false));

        f = union.fields().get(1);
        assertThat(f.fieldId(), is(4));
        assertThat(f.name(), is("bar"));
        assertThat(f.type().name(), is("i32"));
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

        ThriftFileElement element = parse(thrift, Location.get("", "unionWithDefault.thrift"));
        StructElement u = element.unions().get(0);

        assertThat(u.name(), is("Default"));
        assertThat(u.fields().size(), is(4));

        ImmutableList<FieldElement> fields = u.fields();

        FieldElement f = fields.get(0);
        assertThat(f.name(), is("foo"));
        assertThat(f.fieldId(), is(1));
        assertThat(f.type().name(), is("i16"));
        assertThat(f.constValue(), is(nullValue()));

        f = fields.get(1);
        assertThat(f.name(), is("bar"));
        assertThat(f.fieldId(), is(2));
        assertThat(f.type().name(), is("i16"));
        assertThat(f.constValue(), is(nullValue()));

        f = fields.get(2);
        assertThat(f.name(), is("baz"));
        assertThat(f.fieldId(), is(3));
        assertThat(f.type().name(), is("i16"));
        assertThat((Long) f.constValue().value(), is(0xFFFL));

        f = fields.get(3);
        assertThat(f.name(), is("quux"));
        assertThat(f.fieldId(), is(4));
        assertThat(f.type().name(), is("i16"));
        assertThat(f.constValue(), is(nullValue()));
    }

    @Test
    public void simpleConst() {
        String thrift = "const i64 DefaultStatusCode = 200";
        ThriftFileElement file = parse(thrift, Location.get("", "simpleConst.thrift"));
        ConstElement c = file.constants().get(0);

        assertThat(c.name(), is("DefaultStatusCode"));
        assertThat(c.type().name(), is("i64"));
        assertThat(c.value().kind(), is(ConstValueElement.Kind.INTEGER));
        assertThat((Long) c.value().value(), is(200L));
    }

    @Test
    public void listConst() {
        String thrift = "const list<string> Names = [\"foo\" \"bar\", \"baz\"; \"quux\"]";
        ThriftFileElement file = parse(thrift, Location.get("", "listConst.thrift"));
        ConstElement c = file.constants().get(0);

        assertThat(c.name(), is("Names"));
        assertThat(c.type().name(), is("list<string>"));

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

        ThriftFileElement file = parse(thrift, Location.get("", "mapConst.thrift"));
        ConstElement c = file.constants().get(0);

        assertThat(c.name(), is("Headers"));
        assertThat(c.type().name(), is("map<string, string>"));

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

        ThriftFileElement file = parse(thrift);
        StructElement s = file.structs().get(0);
        FieldElement f = s.fields().get(0);
        assertThat(f.fieldId(), is(100));
        assertThat(f.name(), is("num"));
        assertThat(f.type().name(), is("i32"));

        ConstValueElement v = f.constValue();
        assertThat(v, is(notNullValue()));
        assertThat(v.kind(), is(ConstValueElement.Kind.INTEGER));
        assertThat((Long) v.value(), is(1L));
    }

    @Test
    public void canParseOfficialTestCase() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream stream = classLoader.getResourceAsStream("cases/TestThrift.thrift");
        String thrift = Okio.buffer(Okio.source(stream)).readUtf8();
        Location location = Location.get("cases", "TestThrift.thrift");

        ThriftParser.parse(location, thrift);
    }

    @Test
    public void bareEnums() throws Exception {
        String thrift = "enum Enum {\n" +
                "  FOO,\n" +
                "  BAR\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        EnumElement anEnum = file.enums().get(0);
        assertThat(anEnum.name(), is("Enum"));
        assertThat(anEnum.members().size(), is(2));

        EnumMemberElement member = anEnum.members().get(0);
        assertThat(member.name(), is("FOO"));
        assertThat(member.value(), is(0));

        member = anEnum.members().get(1);
        assertThat(member.name(), is("BAR"));
        assertThat(member.value(), is(1));
    }

    @Test
    public void enumWithLargeGaps() throws Exception {
        String thrift = "enum Gaps {\n" +
                "  SMALL = 10,\n" +
                "  MEDIUM = 100,\n" +
                "  ALSO_MEDIUM,\n" +
                "  LARGE = 5000\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        EnumElement anEnum = file.enums().get(0);
        assertThat(anEnum.name(), is("Gaps"));
        assertThat(anEnum.members().size(), is(4));

        List<EnumMemberElement> members = anEnum.members();
        EnumMemberElement member = members.get(0);
        assertThat(member.name(), is("SMALL"));
        assertThat(member.value(), is(10));

        member = members.get(1);
        assertThat(member.name(), is("MEDIUM"));
        assertThat(member.value(), is(100));

        member = members.get(2);
        assertThat(member.name(), is("ALSO_MEDIUM"));
        assertThat(member.value(), is(101));

        member = members.get(3);
        assertThat(member.name(), is("LARGE"));
        assertThat(member.value(), is(5000));
    }

    @Test
    public void defaultValuesCanClash() throws Exception {
        String thrift = "enum Enum {\n" +
                "  FOO = 5,\n" +
                "  BAR = 4,\n" +
                "  BAZ\n" +
                "}";

        try {
            ThriftParser.parse(Location.get("", ""), thrift);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("duplicate enum value"));
        }
    }

    @Test
    public void annotationsOnNamespaces() throws Exception {
        String thrift = "namespace java com.test (foo = 'bar')";
        ThriftFileElement file = parse(thrift);
        NamespaceElement ns = file.namespaces().get(0);

        AnnotationElement ann = ns.annotations();
        assertNotNull(ann);
        assertThat(ann.values().get("foo"), is("bar"));
    }

    @Test
    public void annotationsOnTypedefs() throws Exception {
        String thrift = "namespace java com.test\n" +
                "\n" +
                "typedef i32 StatusCode (boxed = 'false');";

        ThriftFileElement file = parse(thrift);
        TypedefElement typedef = file.typedefs().get(0);
        AnnotationElement ann = typedef.annotations();

        assertNotNull(ann);
        assertThat(ann.get("boxed"), is("false"));
    }

    @Test
    public void annotationsOnEnums() throws Exception {
        String thrift = "enum Foo {} (bar = 'baz')";
        ThriftFileElement file = parse(thrift);
        EnumElement e = file.enums().get(0);
        AnnotationElement ann = e.annotations();

        assertNotNull(ann);
        assertThat(ann.values().get("bar"), is("baz"));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void annotationsOnEnumMembers() throws Exception {
        String thrift = "" +
                "enum Foo {\n" +
                "  BAR (bar = 'abar'),\n" +
                "  BAZ = 1 (baz = 'abaz'),\n" +
                "  QUX (qux = 'aqux')\n" +
                "  WOO\n" +
                "}";

        ThriftFileElement file = parse(thrift);

        EnumElement anEnum = file.enums().get(0);
        EnumMemberElement bar = anEnum.members().get(0);
        EnumMemberElement baz = anEnum.members().get(1);
        EnumMemberElement qux = anEnum.members().get(2);
        EnumMemberElement woo = anEnum.members().get(3);

        assertThat(bar.annotations().get("bar"), is("abar"));
        assertThat(baz.annotations().get("baz"), is("abaz"));
        assertThat(qux.annotations().get("qux"), is("aqux"));
        assertNull(woo.annotations());
    }

    @Test
    public void annotationsOnServices() throws Exception {
        String thrift = "" +
                "service Svc {" +
                "  void foo(1: i32 bar)" +
                "} (async = 'true', java.races = 'false')";
        ThriftFileElement file = parse(thrift);
        ServiceElement svc = file.services().get(0);
        AnnotationElement ann = svc.annotations();

        assertNotNull(ann);
        assertThat(ann.size(), is(2));
        assertThat(ann.get("async"), is("true"));
        assertThat(ann.get("java.races"), is("false"));
    }

    @Test
    public void annotationsOnFunctions() throws Exception {
        String thrift = "service Svc {\n" +
                "  void nothrow() (test = 'a'),\n" +
                "  void nosep() (test = 'b')\n" +
                "  i32 hasThrow() throws(1: string what) (test = 'c');\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        ServiceElement svc = file.services().get(0);
        AnnotationElement a = svc.functions().get(0).annotations();
        AnnotationElement b = svc.functions().get(1).annotations();
        AnnotationElement c = svc.functions().get(2).annotations();

        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);

        assertThat(a.get("test"), is("a"));
        assertThat(b.get("test"), is("b"));
        assertThat(c.get("test"), is("c"));
    }

    @Test
    public void annotationsOnStructs() throws Exception {
        String thrift = "" +
                "struct Str {\n" +
                "  1: i32 hi,\n" +
                "  2: optional string there,\n" +
                "} (layout = 'sequential')";

        ThriftFileElement file = parse(thrift);
        StructElement struct = file.structs().get(0);
        AnnotationElement ann = struct.annotations();

        assertNotNull(ann);
        assertThat(ann.get("layout"), is("sequential"));
    }

    @Test
    public void annotationsOnUnions() throws Exception {
        String thrift = "" +
                "union Union {\n" +
                "  1: i32 hi,\n" +
                "  2: optional string there,\n" +
                "} (layout = 'padded')";

        ThriftFileElement file = parse(thrift);
        StructElement union = file.unions().get(0);
        AnnotationElement ann = union.annotations();

        assertNotNull(ann);
        assertThat(ann.get("layout"), is("padded"));
    }

    @Test
    public void annotationsOnExceptions() throws Exception {
        String thrift = "" +
                "exception Exception {\n" +
                "  1: required i32 boom,\n" +
                "} (java.runtime_exception)";

        ThriftFileElement file = parse(thrift);
        StructElement exception = file.exceptions().get(0);
        AnnotationElement ann = exception.annotations();

        assertNotNull(ann);
        assertThat(ann.get("java.runtime_exception"), is("true"));
    }

    @Test
    public void annotationsOnFields() {
        String thrift = "struct Str {\n" +
                "  1: i32 what (what = 'what'),\n" +
                "  2: binary data (compression = 'zlib') // doc\n" +
                "  3: optional i8 bits (synonym = 'byte')\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        StructElement struct = file.structs().get(0);

        AnnotationElement anno = struct.fields().get(0).annotations();
        assertNotNull(anno);
        assertThat(anno.get("what"), is("what"));

        anno = struct.fields().get(1).annotations();
        assertNotNull(anno);
        assertThat(anno.get("compression"), is("zlib"));

        anno = struct.fields().get(2).annotations();
        assertNotNull(anno);
        assertThat(anno.get("synonym"), is("byte"));
    }

    @Test
    public void annotationsOnFieldTypes() {
        String thrift = "struct Str {\n" +
                "  1: map<string, i32> (python.immutable) foo\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        StructElement struct = file.structs().get(0);
        FieldElement field = struct.fields().get(0);
        AnnotationElement anno = field.type().annotations();

        assertNotNull(anno);
        assertThat(anno.get("python.immutable"), is("true"));
    }

    @Test
    public void annotationsOnConsecutiveDefinitions() throws Exception {
        String thrift = "" +
                "namespace java com.foo.bar (ns = 'ok')\n" +
                "enum Foo {} (enumAnno = 'yep')\n" +
                "";

        parse(thrift);
    }
    
    private static ThriftFileElement parse(String thrift) {
        return parse(thrift, Location.get("", ""));
    }

    private static ThriftFileElement parse(String thrift, Location location) {
        return ThriftParser.parse(location, thrift);
    }

}
