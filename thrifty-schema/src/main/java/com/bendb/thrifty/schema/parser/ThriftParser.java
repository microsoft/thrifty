/*
 * Copyright (C) 2015 Benjamin Bader
 * Copyright (C) 2013 Square, Inc.
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

import com.bendb.thrifty.schema.JavadocUtil;
import com.bendb.thrifty.schema.Location;
import com.bendb.thrifty.schema.NamespaceScope;
import com.bendb.thrifty.schema.Requiredness;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.Set;

import javax.annotation.Nullable;

public final class ThriftParser {
    private final Location location;
    private final char[] data;
    private final ImmutableList.Builder<NamespaceElement> namespaces = ImmutableList.builder();
    private final ImmutableList.Builder<IncludeElement> imports = ImmutableList.builder();
    private final ImmutableList.Builder<EnumElement> enums = ImmutableList.builder();
    private final ImmutableList.Builder<ConstElement> consts = ImmutableList.builder();
    private final ImmutableList.Builder<StructElement> structs = ImmutableList.builder();
    private final ImmutableList.Builder<StructElement> unions = ImmutableList.builder();
    private final ImmutableList.Builder<StructElement> exceptions = ImmutableList.builder();
    private final ImmutableList.Builder<ServiceElement> services = ImmutableList.builder();
    private final ImmutableList.Builder<TypedefElement> typedefs = ImmutableList.builder();

    private boolean readingHeaders = true;
    private int declCount = 0;
    private int pos;
    private int line;
    private int lineStart;

    /**
     * Parse the given Thrift {@code text}, using the given {@code location}
     * to anchor parsed elements withing the file.
     * @param location the {@link Location} of the data being parsed.
     * @param text the text to be parsed.
     * @return a representation of the parsed Thrift data.
     */
    public static ThriftFileElement parse(Location location, String text) {
        return new ThriftParser(location, text.toCharArray()).readThriftData();
    }

    private ThriftParser(Location location, char[] data) {
        this.location = Preconditions.checkNotNull(location, "location");
        this.data = Preconditions.checkNotNull(data, "data");
    }

    private ThriftFileElement readThriftData() {
        //noinspection InfiniteLoopStatement
        while (true) {
            String doc = readDocumentation();
            if (pos == data.length) {
                return ThriftFileElement.builder(location)
                        .namespaces(namespaces.build())
                        .includes(imports.build())
                        .constants(consts.build())
                        .typedefs(typedefs.build())
                        .enums(enums.build())
                        .structs(structs.build())
                        .unions(unions.build())
                        .exceptions(exceptions.build())
                        .services(services.build())
                        .build();
            }

            Object element = readElement(doc);

            if (isHeader(element)) {
                if (!readingHeaders) {
                    throw unexpected("namespace and import statements must precede all other declarations");
                }
            } else {
                readingHeaders = false;
            }

            if (element instanceof NamespaceElement) {
                if (((NamespaceElement) element).scope() != NamespaceScope.UNKNOWN) {
                    namespaces.add((NamespaceElement) element);
                }
            } else if (element instanceof IncludeElement) {
                imports.add((IncludeElement) element);
            } else if (element instanceof EnumElement) {
                enums.add((EnumElement) element);
            } else if (element instanceof ConstElement) {
                consts.add((ConstElement) element);
            } else if (element instanceof StructElement) {
                StructElement struct = (StructElement) element;
                switch (struct.type()) {
                    case STRUCT:    structs.add(struct);    break;
                    case UNION:     unions.add(struct);     break;
                    case EXCEPTION: exceptions.add(struct); break;
                    default:
                        throw new AssertionError("Unexpected struct type: " + struct.type().name());
                }
            } else if (element instanceof ServiceElement) {
                services.add((ServiceElement) element);
            } else if (element instanceof TypedefElement) {
                typedefs.add((TypedefElement) element);
            }
        }
    }

    private static boolean isHeader(Object element) {
        return element instanceof NamespaceElement
                || element instanceof IncludeElement;
    }

    private Object readElement(String doc) {
        Location location = location();
        String word = readWord();

        if ("namespace".equals(word)) {
            String scopeName = readNamespaceScope();
            NamespaceScope scope = NamespaceScope.forThriftName(scopeName);

            String namespace;
            if (scope == NamespaceScope.SMALLTALK_CATEGORY) {
                namespace = readSmalltalkIdentifier();
            } else {
                namespace = readIdentifier();
            }

            if (scope == null) {
                // TODO: Implement warnings
                scope = NamespaceScope.UNKNOWN;
            }

            AnnotationElement ann = readAnnotations();

            return NamespaceElement.builder(location)
                    .scope(scope)
                    .namespace(namespace)
                    .annotations(ann)
                    .build();
        }

        if ("php_namespace".equals(word)) {
            String namespace = readLiteral();
            return NamespaceElement.builder(location)
                    .scope(NamespaceScope.PHP)
                    .namespace(namespace)
                    .build();
        }

        if ("xsd_namespace".equals(word)) {
            throw unexpected("xsd_syntax is not supported");
        }

        if ("include".equals(word)) {
            String path = readLiteral();
            return IncludeElement.create(location, false, path);
        }

        if ("cpp_include".equals(word)) {
            String path = readLiteral();
            return IncludeElement.create(location, true, path);
        }

        if ("const".equals(word)) {
            ++declCount;
            return readConst(location, doc);
        }

        if ("typedef".equals(word)) {
            ++declCount;
            TypeElement oldType = readTypeName();
            String newName = readWord();
            AnnotationElement annotations = readAnnotations();

            doc = readTrailingDoc(doc, true);

            return TypedefElement.builder(location)
                    .documentation(formatJavadoc(doc))
                    .oldType(oldType)
                    .newName(newName)
                    .annotations(annotations)
                    .build();
        }

        if ("enum".equals(word)) {
            ++declCount;
            return readEnum(location, doc);
        }

        if ("senum".equals(word)) {
            throw unexpected("senum has been deprecated and is not supported.");
        }

        if ("struct".equals(word)) {
            ++declCount;
            return readStruct(location, doc);
        }

        if ("union".equals(word)) {
            ++declCount;
            return readUnion(location, doc);
        }

        if ("exception".equals(word)) {
            ++declCount;
            return readException(location, doc);
        }

        if ("service".equals(word)) {
            ++declCount;
            return readService(location, doc);
        }

        throw unexpected("unexpected element: " + word);
    }

    private EnumElement readEnum(Location location, String documentation) {
        String name = readIdentifier();

        if (readChar() != '{') {
            throw unexpected("expected an opening brace in enum definition for: " + name);
        }

        int nextId = 0;
        Set<Integer> ids = Sets.newHashSet();
        ImmutableList.Builder<EnumMemberElement> members = ImmutableList.builder();
        while (true) {
            String memberDoc = readDocumentation();
            if (peekChar() == '}') {
                ++pos;
                break;
            }

            EnumMemberElement member = readEnumMember(memberDoc);

            int value;
            if (member.value() == null) {
                value = nextId++;
                member = member.withValue(value);
            } else {
                //noinspection ConstantConditions
                value = member.value();
                nextId = value + 1;
            }

            if (!ids.add(value)) {
                throw unexpected(member.location(), "duplicate enum value: " + value);
            }

            members.add(member);
        }

        return EnumElement.builder(location)
                .documentation(formatJavadoc(documentation))
                .name(name)
                .members(members.build())
                .annotations(readAnnotations())
                .build();
    }

    private EnumMemberElement readEnumMember(String doc) {
        // enum member:
        //   identifier ('=' IntValue)? Separator? Comment? '\n'
        Location location = location();
        String name = readIdentifier();

        char next = peekChar(false);
        Integer value = null;
        if (next == '=') {
            ++pos;
            value = readInt();
        }

        AnnotationElement annotation = readAnnotations();

        doc = readTrailingDoc(doc, true);

        return EnumMemberElement.builder(location)
                .documentation(formatJavadoc(doc))
                .name(name)
                .value(value)
                .annotations(annotation)
                .build();
    }

    private ConstElement readConst(Location location, String documentation) {
        TypeElement typeName = readTypeName();
        String name = readIdentifier();

        if (readChar() != '=') {
            throw unexpected("expected a constant value for const: " + name);
        }

        ConstValueElement value = readConstValue();

        documentation = readTrailingDoc(documentation, true);

        // Don't bother validating the value type against the typename -
        // we'll catch that later.
        return ConstElement.builder(location)
                .documentation(formatJavadoc(documentation))
                .type(typeName)
                .name(name)
                .value(value)
                .build();
    }

    private StructElement readStruct(Location location, String documentation) {
        return readAggregateType(location, documentation, StructElement.Type.STRUCT);
    }

    private StructElement readUnion(Location location, String documentation) {
        StructElement element = readAggregateType(location, documentation, StructElement.Type.UNION);
        boolean hasDefaultField = false;
        for (FieldElement field : element.fields()) {
            if (field.requiredness() == Requiredness.REQUIRED) {
                throw unexpected("unions cannot have required fields: " + field.name());
            }

            if (field.constValue() != null) {
                if (hasDefaultField) {
                    throw unexpected("unions can have at most one default value");
                }
                hasDefaultField = true;
            }
        }
        return element;
    }

    private StructElement readException(Location location, String documentation) {
        return readAggregateType(location, documentation, StructElement.Type.EXCEPTION);
    }

    private StructElement readAggregateType(Location location, String documentation, StructElement.Type type) {
        String name = readIdentifier();

        // Deliberately not supporting xsd_all.
        if (readChar() != '{') {
            throw unexpected("expected an opening brace in struct definition for: " + name);
        }

        ImmutableList<FieldElement> fields = readFieldList('}', false);
        AnnotationElement annotations = readAnnotations();

        return StructElement.builder(location)
                .documentation(formatJavadoc(documentation))
                .type(type)
                .name(name)
                .fields(fields)
                .annotations(annotations)
                .build();
    }

    private ImmutableList<FieldElement> readFieldList(char terminator, boolean requiredByDefault) {
        int currentId = 1;
        Set<Integer> ids = Sets.newHashSet();

        ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();
        while (true) {
            String fieldDoc = readDocumentation();
            if (peekChar() == terminator) {
                ++pos;
                break;
            }

            FieldElement field = readField(fieldDoc, requiredByDefault);

            Integer id = field.fieldId();
            if (id != null) {
                if (id < 1) {
                    throw unexpected("field ID must be a positive integer");
                }

                if (!ids.add(id)) {
                    throw unexpected("duplicate field ID: " + id);
                }

                if (id >= currentId) {
                    currentId = id + 1;
                }
            } else {
                int fieldId = currentId++;
                if (!ids.add(fieldId)) {
                    throw unexpected("duplicate field ID: " + fieldId);
                }

                field = field.withId(fieldId);
            }

            fields.add(field);
        }

        return fields.build();
    }

    private FieldElement readField(String doc, boolean requiredByDefault) {
        if (pos == data.length) {
            throw new AssertionError();
        }

        FieldElement.Builder field = FieldElement.builder(location())
                .documentation(formatJavadoc(doc));

        if ((data[pos] >= '0' && data[pos] <= '9') || data[pos] == '-') {
            Integer fieldId;
            try {
                fieldId = readInt();
            } catch (Exception e) {
                throw unexpected("invalid field ID: " + e.getMessage());
            }

            if (fieldId < 1) {
                throw unexpected("field ID must be greater than zero, was: " + fieldId);
            }

            if (readChar() != ':') {
                throw unexpected("expected a ':' separator");
            }

            field.fieldId(fieldId);
        }

        TypeElement typeName = readTypeName();

        Requiredness requiredness = requiredByDefault
                ? Requiredness.REQUIRED
                : Requiredness.DEFAULT;

        if ("required".equals(typeName.name())) {
            requiredness = Requiredness.REQUIRED;
            typeName = readTypeName();
        } else if ("optional".equals(typeName.name())) {
            requiredness = Requiredness.OPTIONAL;
            typeName = readTypeName();
        }

        field.requiredness(requiredness);
        field.type(typeName);
        field.name(readIdentifier());

        char next = peekChar(false);
        if (next == '=') {
            readChar();
            field.constValue(readConstValue());
        }

        AnnotationElement annotations = readAnnotations();
        field.annotations(annotations);

        doc = readTrailingDoc(doc, true);
        if (JavadocUtil.isNonEmptyJavadoc(doc)) {
            field.documentation(formatJavadoc(doc));
        }

        return field.build();
    }

    private ServiceElement readService(Location location, String doc) {
        String name = readIdentifier();
        TypeElement extendsType = null;

        if (peekChar() == 'e') {
            String word = readWord();
            if (!"extends".equals(word)) {
                throw unexpected("unexpected token: " + word);
            }

            extendsType = readTypeName();

            if (extendsType == null) {
                throw unexpected("expected a type name");
            }

            if (!(extendsType instanceof ScalarTypeElement)) {
                throw unexpected("services cannot extend collections");
            }
        }

        if (readChar() != '{') {
            throw unexpected("expected an opening brace in service definition");
        }

        ImmutableList<FunctionElement> functions = readFunctionList();
        AnnotationElement annotations = readAnnotations();

        return ServiceElement.builder(location)
                .documentation(formatJavadoc(doc))
                .name(name)
                .extendsService(extendsType)
                .functions(functions)
                .annotations(annotations)
                .build();
    }

    private ImmutableList<FunctionElement> readFunctionList() {
        ImmutableList.Builder<FunctionElement> funcs = ImmutableList.builder();
        while (true) {
            String functionDoc = readDocumentation();
            if (peekChar() == '}') {
                ++pos;
                return funcs.build();
            }

            FunctionElement func = readFunction(location(), functionDoc);

            funcs.add(func);
        }
    }

    private FunctionElement readFunction(Location location, String functionDoc) {
        FunctionElement.Builder func = FunctionElement.builder(location)
                .documentation(formatJavadoc(functionDoc));

        TypeElement returnType = readTypeName();
        if ("oneway".equals(returnType.name())) {
            func.oneWay(true);
            returnType = readTypeName();
        }

        func.returnType(returnType);
        func.name(readIdentifier());

        if (readChar() != '(') {
            throw unexpected("invalid function definition");
        }

        func.params(readFieldList(')', true));

        char next = peekCharSameLine();
        if (next == 't') {
            String word = readWord();

            if (!"throws".equals(word)) {
                throw unexpected("unexpected token in function definition: " + word);
            }

            if (readChar() != '(') {
                throw unexpected("expected a list of exception types after 'throws'");
            }

            func.exceptions(readFieldList(')', false));
        }

        AnnotationElement annotations = readAnnotations();
        func.annotations(annotations);

        functionDoc = readTrailingDoc(functionDoc, true);
        if (JavadocUtil.isNonEmptyJavadoc(functionDoc)) {
            func.documentation(formatJavadoc(functionDoc));
        }

        return func.build();
    }

    private ConstValueElement readConstValue() {
        skipWhitespace(false);
        Location location = location();
        char c = peekChar(false);
        if (c == '"' || c == '\'') {
            String literal = readLiteral();
            return ConstValueElement.literal(location, literal);
        }

        if (c == '[') {
            ImmutableList.Builder<ConstValueElement> values = ImmutableList.builder();
            readChar();
            while (peekChar(false) != ']') {
                values.add(readConstValue());

                char maybeSeparator = peekChar(false);
                if (maybeSeparator == ',' || maybeSeparator == ';') {
                    readChar();
                }
            }

            readChar();
            return ConstValueElement.list(location, values.build());
        }

        if (c == '{') {
            ImmutableMap.Builder<ConstValueElement, ConstValueElement> map = ImmutableMap.builder();
            readChar();
            while (peekChar(false) != '}') {
                ConstValueElement key = readConstValue();
                if (readChar() != ':') {
                    throw unexpected("expected ':' in a map literal");
                }
                ConstValueElement value = readConstValue();
                map.put(key, value);

                char maybeSeparator = peekChar(false);
                if (maybeSeparator == ',' || maybeSeparator == ';') {
                    readChar();
                }
            }

            readChar();
            return ConstValueElement.map(location, map.build());
        }

        if (c == '+' || c == '-' || (c >= '0' && c <= '9')) {
            String number = readWord();
            if (number.indexOf('.') != -1) {
                try {
                    double d = Double.parseDouble(number);
                    return ConstValueElement.real(location, d);
                } catch (NumberFormatException e) {
                    throw unexpected(location, "invalid double constant: " + number);
                }
            } else {
                try {
                    int radix = 10;
                    if (number.startsWith("0x") || number.startsWith("0X")) {
                        number = number.substring(2);
                        radix = 16;
                    }
                    int i = Integer.parseInt(number, radix);
                    return ConstValueElement.integer(location, i);
                } catch (NumberFormatException e) {
                    throw unexpected(location, "invalid integer constant: " + number);
                }
            }
        }

        String id = readIdentifier();
        return ConstValueElement.identifier(location, id);
    }

    @Nullable
    private AnnotationElement readAnnotations() {
        while (pos < data.length) {
            char c = data[pos];
            if (c == ' ' || c == '\t') {
                ++pos;
            } else {
                break;
            }
        }

        if (pos == data.length) {
            return null;
        }

        char c = data[pos];
        if (c != '(') {
            return null;
        }

        Location loc = location();
        readChar();

        ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
        while (pos < data.length) {
            c = data[pos];
            if (c == ')') {
                ++pos;
                return AnnotationElement.create(loc, values.build());
            }

            String name = readIdentifier();
            String value = "true";
            if (peekChar() == '=') {
                readChar();
                value = readLiteral();
            }

            c = peekChar();
            if (c == ',' || c == ';') {
                readChar();
            }

            values.put(name, value);
        }

        throw unexpected("unexpected end of input");
    }

    private char readChar() {
        char c = peekChar();
        pos++;
        return c;
    }

    /**
     * Returns the next non-whitespace, non-comment character, without
     * consuming it.
     */
    private char peekChar() {
        return peekChar(true);
    }

    /**
     * Returns the next non-whitespace character, optionally skipping over
     * comments, without consuming the returned character.
     *
     * @param skipComments {@code true} to consume and ignore comments.
     */
    private char peekChar(boolean skipComments) {
        skipWhitespace(skipComments);
        if (pos == data.length) {
            throw unexpected("unexpected end of file");
        }
        return data[pos];
    }

    /**
     * Skips non-newline whitespace characters, returning either
     * the first non-whitespace character on the current line or the first
     * newline character encountered.
     *
     * When this method returns, {@link #pos} is positioned at the returned
     * character.
     */
    private char peekCharSameLine() {
        while (pos < data.length) {
            char c = data[pos];
            if (c == ' ' || c == '\t') {
                pos++;
            } else {
                return c;
            }
        }

        throw unexpected("Unexpected end of input");
    }

    private String readTrailingDoc(String doc, boolean consumeSeparator) {
        boolean acceptSeparator = consumeSeparator;
        int commentType = -1;
        while (pos < data.length) {
            char c = data[pos];
            if (acceptSeparator && (c == ',' || c == ';')) {
                ++pos;
                acceptSeparator = false;
            } else if (c == ' ' || c == '\t') {
                ++pos;
            } else if (c == '#' || c == '/') {
                ++pos;
                commentType = c;
                break;
            } else {
                break;
            }
        }

        if (commentType == -1) {
            return doc;
        }

        if (commentType == '/') {
            if (pos == data.length || (data[pos] != '/' && data[pos] != '*')) {
                --pos;
                throw unexpected("expected '//' or '/*'");
            }

            commentType = data[pos++];
        }

        int start = pos;
        int end;
        if (commentType == '*') {
            while (true) {
                if (pos == data.length || data[pos] == '\n') {
                    throw unexpected("trailing comment must be closed on the same line");
                }
                if (data[pos] == '*' && pos + 1 < data.length && data[pos + 1] == '/') {
                    end = pos - 1;
                    pos += 2;
                    break;
                }
                ++pos;
            }

            // Only whitespace can follow a trailing star comment
            while (pos < data.length) {
                char c = data[pos++];
                if (c == '\n') {
                    newline();
                    break;
                }

                if (c != ' ' && c != '\t') {
                    throw unexpected("no syntax may follow trailing comment");
                }
            }
        } else {
            while (true) {
                if (pos == data.length) {
                    end = pos - 1;
                    break;
                }

                char c = data[pos++];
                if (c == '\n') {
                    newline();
                    end = pos - 2;
                    break;
                }
            }
        }

        while (end > start && (data[end] == ' ' || data[end] == '\t')) {
            --end;
        }

        while (start < end && (data[start] == ' ' || data[start] == '\t')) {
            ++start;
        }

        if (end == start) {
            return doc;
        }

        int trailingLength = end - start + 1;
        StringBuilder result = new StringBuilder(doc.length() + trailingLength);
        result.append(doc);
        if (!Strings.isNullOrEmpty(doc) && !doc.endsWith("\n")) {
            result.append('\n');
        }
        result.append(data, start, trailingLength);

        if (result.charAt(result.length() - 1) != '\n') {
            result.append('\n');
        }

        return result.toString();
    }

    private String readLiteral() {
        skipWhitespace(true);
        char quote = readChar();
        if (quote != '"' && quote != '\'') {
            throw new AssertionError();
        }

        StringBuilder sb = new StringBuilder();
        while (pos < data.length) {
            char c = data[pos++];
            if (c == quote) {
                return sb.toString();
            }

            if (c == '\\') {
                if (pos == data.length) {
                    throw unexpected("Unexpected end of input");
                }

                char escape = data[pos++];
                switch (escape) {
                    case 'a':
                        sb.append((char) 0x7);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'v':
                        sb.append((char) 0xB);
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    default:
                        if (escape == quote) {
                            sb.append(quote);
                        } else {
                            throw unexpected("invalid escape character: " + escape);
                        }
                }
            } else {
                if (c == '\n') {
                    newline();
                }

                sb.append(c);
            }
        }

        throw unexpected("unterminated string");
    }

    private TypeElement readTypeName() {
        skipWhitespace(false);
        Location loc = location();
        String name = readWord();
        if ("list".equals(name) || "set".equals(name)) {
            if (readChar() != '<') {
                throw unexpected("missing type parameter");
            }
            TypeElement param = readTypeName();
            if (readChar() != '>') {
                throw unexpected("missing closing '>' in parameter list");
            }

            AnnotationElement annotations = readAnnotations();
            return "list".equals(name)
                    ? TypeElement.list(loc, param, annotations)
                    : TypeElement.set(loc, param, annotations);
        }

        if ("map".equals(name)) {
            if (readChar() != '<') {
                throw unexpected("missing type parameter list");
            }

            TypeElement keyType = readTypeName();
            if (readChar() != ',') {
                throw unexpected("invalid map-type parameter list");
            }

            TypeElement valueType = readTypeName();
            if (readChar() != '>') {
                throw unexpected("missing closing '>' in parameter list");
            }

            AnnotationElement annotations = readAnnotations();

            return TypeElement.map(loc, keyType, valueType, annotations);
        }

        return TypeElement.scalar(loc, name, readAnnotations());
    }

    private String readNamespaceScope() {
        skipWhitespace(true);
        if (pos == data.length) {
            throw unexpected("unexpected end of input");
        }
        if (data[pos] == '*') {
            ++pos;
            return "*";
        }
        return readWord();
    }

    private String readIdentifier() {
        return readIdentifier(false);
    }

    private String readSmalltalkIdentifier() {
        return readIdentifier(true);
    }

    private String readIdentifier(boolean allowSmalltalk) {
        skipWhitespace(true);
        int start = pos;
        while (pos < data.length) {
            char c = data[pos];
            if ((c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c == '_')
                || (c >= '0' && c <= '9' && pos > start)
                || (c == '.' && pos > start)
                || (c == '-' && allowSmalltalk && pos > start)) {
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw unexpected("expected an identifier");
        }
        return new String(data, start, pos - start);
    }

    private String readWord() {
        skipWhitespace(true);
        int start = pos;
        while (pos < data.length) {
            char c = data[pos];
            if ((c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || (c == '_')
                || (c == '-')
                || (c == '+')
                || (c == '.')) {
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw unexpected("expected a word");
        }
        return new String(data, start, pos - start);
    }

    private int readInt() {
        skipWhitespace(false);
        int start = pos;
        while (pos < data.length) {
            char c = data[pos];
            if ((c >= '0' && c <= '9')
                    || (c == '+')
                    || (c == '-')
                    || (c == 'x') // for hex prefix
                    || (c == '.')) {
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw unexpected("unexpected end of input");
        }
        String text = new String(data, start, pos - start);
        try {
            int radix = text.startsWith("0x") ? 16 : 10;
            return Integer.parseInt(text, radix);
        } catch (Exception e) {
            throw unexpected("expected an integer but was " + text);
        }
    }

    private String readDocumentation() {
        StringBuilder result = null;
        while (true) {
            skipWhitespace(false);
            if (pos == data.length || (data[pos] != '/' && data[pos] != '#')) {
                return result != null ? result.toString() : "";
            }

            String comment = readComment();
            if (result == null) {
                result = new StringBuilder(comment);
            } else {
                result.append("\n").append(comment);
            }
        }
    }

    private String readComment() {
        if (pos == data.length || (data[pos] != '/' && data[pos] != '#')) {
            throw new AssertionError();
        }

        int commentType;
        if (data[pos] == '#') {
            commentType = '#';
        } else {
            pos++;
            commentType = pos < data.length ? data[pos] : -1;
        }

        // position pos at one char past the start of the comment
        if (commentType != -1) {
            pos++;
        }

        if (commentType == '*') {
            // make a multiline comment
            StringBuilder sb = new StringBuilder();
            boolean startOfLine = true;

            for (; pos + 1 < data.length; pos++) {
                char c = data[pos];
                if (c == '*' && data[pos + 1] == '/') {
                    pos += 2;
                    return sb.toString().trim();
                }

                if (c == '\n') {
                    sb.append('\n');
                    newline();
                    startOfLine = true;
                } else if (!startOfLine) {
                    sb.append(c);
                } else if (c == '*') {
                    if (data[pos + 1] == ' ') {
                        pos += 1; // skip a single leading space
                    }
                    startOfLine = false;
                } else if (!Character.isWhitespace(c)) {
                    sb.append(c);
                    startOfLine = false;
                }
            }
            throw unexpected("unterminated comment");
        } else if (commentType == '/' || commentType == '#') {
            // make a single-line comment
            if (pos < data.length && data[pos] == ' ') {
                // skip a single leading space
                pos++;
            }
            int start = pos;
            while (pos < data.length) {
                char c = data[pos++];
                if (c == '\n') {
                    newline();
                    break;
                }
            }
            return new String(data, start, pos - 1 - start);
        } else {
            throw unexpected("Unexpected start-of-comment");
        }
    }

    private void skipWhitespace(boolean skipComments) {
        while (pos < data.length) {
            char c = data[pos];
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                pos++;
                if (c == '\n') {
                    newline();
                }
            } else if (skipComments && (c == '/' || c == '#')) {
                readComment();
            } else {
                break;
            }
        }
    }

    private void newline() {
        line++;
        lineStart = pos;
    }

    private static String formatJavadoc(String doc) {
        if (Strings.isNullOrEmpty(doc)) {
            return doc;
        }
        return doc.endsWith("\n") ? doc : doc + "\n";
    }

    private Location location() {
        return location.at(line + 1, pos - lineStart + 1);
    }

    private RuntimeException unexpected(String message) {
        return unexpected(location(), message);
    }

    private RuntimeException unexpected(Location location, String message) {
        throw new IllegalStateException(String.format("Syntax error in %s: %s", location, message));
    }
}
