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
package com.microsoft.thrifty.schema.parser;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.microsoft.thrifty.schema.ErrorReporter;
import com.microsoft.thrifty.schema.JavadocUtil;
import com.microsoft.thrifty.schema.Location;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.Requiredness;

import javax.annotation.Nullable;
import java.util.Set;

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
    private int pos;
    private int line;
    private int lineStart;

    private ErrorReporter errorReporter;

    /**
     * Parse the given Thrift {@code text}, using the given {@code location}
     * to anchor parsed elements withing the file.
     * @param location the {@link Location} of the data being parsed.
     * @param text the text to be parsed.
     * @return a representation of the parsed Thrift data.
     */
    public static ThriftFileElement parse(Location location, String text) {
        return parse(location, text, new ErrorReporter());
    }

    /**
     * Parse the given Thrift {@code text}, using the given {@code location}
     * to anchor parsed elements withing the file.
     * @param location the {@link Location} of the data being parsed.
     * @param text the text to be parsed.
     * @param reporter an {@link ErrorReporter} to collect warnings.
     * @return a representation of the parsed Thrift data.
     */
    public static ThriftFileElement parse(Location location, String text, ErrorReporter reporter) {
        ThriftParser parser = new ThriftParser(location, text.toCharArray());
        parser.errorReporter = reporter;
        return parser.readThriftData();
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
            Location scopeNameLocation = location();
            String scopeName = readNamespaceScope();
            NamespaceScope scope = NamespaceScope.forThriftName(scopeName);

            String namespace;
            if (scope == NamespaceScope.SMALLTALK_CATEGORY) {
                namespace = readSmalltalkIdentifier();
            } else {
                namespace = readIdentifier();
            }

            if (scope == null) {
                errorReporter.warn(scopeNameLocation, "Unknown namespace scope '" + scopeName + "'");
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
            String path = readLiteral(false); // Don't process escapes, Windows paths will fail to resolve.
            return IncludeElement.create(location, false, path);
        }

        if ("cpp_include".equals(word)) {
            String path = readLiteral();
            return IncludeElement.create(location, true, path);
        }

        if ("const".equals(word)) {
            return readConst(location, doc);
        }

        if ("typedef".equals(word)) {
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
            return readEnum(location, doc);
        }

        if ("senum".equals(word)) {
            throw unexpected("senum has been deprecated and is not supported.");
        }

        if ("struct".equals(word)) {
            return readStruct(location, doc);
        }

        if ("union".equals(word)) {
            return readUnion(location, doc);
        }

        if ("exception".equals(word)) {
            return readException(location, doc);
        }

        if ("service".equals(word)) {
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

            EnumMemberElement member = readEnumMember(memberDoc, nextId);

            // value is either the default, or an explicit number.  Either way, the next default is n + 1.
            nextId = member.value() + 1;

            if (!ids.add(member.value())) {
                throw unexpected(member.location(), "duplicate enum value: " + member.value());
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

    private EnumMemberElement readEnumMember(String doc, int defaultValue) {
        // enum member:
        //   identifier ('=' IntValue)? Separator? Comment? '\n'
        Location location = location();
        String name = readIdentifier();

        char next = peekChar(false);
        int value = defaultValue;
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
        int nextId = 1;
        Set<Integer> ids = Sets.newHashSet();

        ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();
        while (true) {
            String fieldDoc = readDocumentation();
            if (peekChar() == terminator) {
                ++pos;
                break;
            }

            FieldElement field = readField(fieldDoc, requiredByDefault, nextId);

            int id = field.fieldId();
            if (id < 1) {
                throw unexpected("field ID must be a positive integer");
            }

            if (!ids.add(id)) {
                throw unexpected("duplicate field ID: " + field.fieldId());
            }

            if (id >= nextId) {
                nextId = id + 1;
            }

            fields.add(field);
        }

        return fields.build();
    }

    private FieldElement readField(String doc, boolean requiredByDefault, int defaultFieldId) {
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
        } else {
            field.fieldId(defaultFieldId);
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

        // Workaround for issue #38, where:
        // a) field does not have annotations
        // b) field does not end with a separator
        // c) field does not have trailing doc
        // d) the following field has a doc comment
        //
        // In this case, peeking ahead for annotations consumes newlines.
        // If no annotation or separator is found, then the cursor is positioned
        // at the start of the next field's documentation; we will treat it as
        // trailing documentation of the previous field.  If the doc spans more
        // than one line we'll fail with an exception - if not, the doc will just
        // silently go to the wrong field!
        //
        // The workaround is to remember what line we are on before searching for
        // annotations, and only attempt to read trailing doc if we are on the same
        // line OR we have read at least one annotation (which may be on a separate
        // line).
        //
        // Of course, now this means that the following legal syntax fails:
        // 1: required string foo // this comment is treated as trailing
        //        (annotation = "legal") // but *this* is the real trailing doc
        //
        // This is why some people use parser generators... (issue #30)
        int currentLine = location().line();

        char next = peekChar(false);
        if (next == '=') {
            readChar();
            field.constValue(readConstValue());
        }

        AnnotationElement annotations = readAnnotations();
        field.annotations(annotations);

        if (location().line() == currentLine || annotations != null) {
            doc = readTrailingDoc(doc, true);
            if (JavadocUtil.isNonEmptyJavadoc(doc)) {
                field.documentation(formatJavadoc(doc));
            }
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

        boolean canHaveTrailingElements = true;
        char next = peekCharSameLine();

        if (next == '\n' || next == '\r') {
            canHaveTrailingElements = false;
            next = peekChar(false);
        }

        if (next == 't') {
            String word = peekWord();

            if (!"throws".equals(word)) {
                if (!canHaveTrailingElements) {
                    // we've passed a newline and don't have 'throws' - assume a new
                    // function definition.
                    return func.build();
                } else {
                    throw unexpected("unexpected token in function definition: " + word);
                }
            }
            readWord();

            if (readChar() != '(') {
                throw unexpected("expected a list of exception types after 'throws'");
            }

            func.exceptions(readFieldList(')', false));
            canHaveTrailingElements = true;
        }

        if (canHaveTrailingElements) {
            AnnotationElement annotations = readAnnotations();
            func.annotations(annotations);

            functionDoc = readTrailingDoc(functionDoc, true);
            if (JavadocUtil.isNonEmptyJavadoc(functionDoc)) {
                func.documentation(formatJavadoc(functionDoc));
            }
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
                    long value = Long.parseLong(number, radix);
                    return ConstValueElement.integer(location, value);
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
                return doc;
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
        return readLiteral(true);
    }

    private String readLiteral(boolean processEscapes) {
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

            if (c == '\\' && processEscapes) {
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

    private String peekWord() {
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
        String result = new String(data, start, pos - start);
        pos = start;
        return result;
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
        errorReporter.error(location, message);
        throw new IllegalStateException(String.format("Syntax error in %s: %s", location, message));
    }
}
