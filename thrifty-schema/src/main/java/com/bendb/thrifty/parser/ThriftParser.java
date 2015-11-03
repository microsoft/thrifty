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
package com.bendb.thrifty.parser;

import autovalue.shaded.com.google.common.common.base.Preconditions;
import com.bendb.thrifty.Location;
import com.bendb.thrifty.NamespaceScope;
import com.google.common.collect.ImmutableList;

public final class ThriftParser {
    private final Location location;
    private final char[] data;
    private final ImmutableList.Builder<NamespaceElement> namespaces = ImmutableList.builder();
    private final ImmutableList.Builder<IncludeElement> imports = ImmutableList.builder();
    private final ImmutableList.Builder<EnumElement> enums = ImmutableList.builder();
    private final ImmutableList.Builder<ConstElement> consts = ImmutableList.builder();
    private final ImmutableList.Builder<StructElement> structs = ImmutableList.builder();
    private final ImmutableList.Builder<ServiceElement> services = ImmutableList.builder();
    private final ImmutableList.Builder<TypedefElement> typedefs = ImmutableList.builder();

    private boolean readingHeaders = true;
    private int declCount = 0;
    private int pos;
    private int line;
    private int lineStart;

    ThriftParser(Location location, char[] data) {
        this.location = Preconditions.checkNotNull(location, "location");
        this.data = Preconditions.checkNotNull(data, "data");
    }

    public static ThriftFileElement parse(Location location, String text) {
        return new ThriftParser(location, text.toCharArray()).readThriftData();
    }

    ThriftFileElement readThriftData() {
        //noinspection InfiniteLoopStatement
        while (true) {
            String doc = readDocumentation();
            if (pos == data.length) {
                return buildFileElement();
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
                namespaces.add((NamespaceElement) element);
            } else if (element instanceof IncludeElement) {
                imports.add((IncludeElement) element);
            } else if (element instanceof EnumElement) {
                enums.add((EnumElement) element);
            } else if (element instanceof ConstElement) {
                consts.add((ConstElement) element);
            } else if (element instanceof StructElement) {
                structs.add((StructElement) element);
            } else if (element instanceof ServiceElement) {
                services.add((ServiceElement) element);
            } else if (element instanceof TypedefElement) {
                typedefs.add((TypedefElement) element);
            }
        }
    }

    private ThriftFileElement buildFileElement() {
        ThriftFileElement.Builder builder = ThriftFileElement.builder(location)
                .namespaces(namespaces.build())
                .includes(imports.build())
                .constants(consts.build())
                .typedefs(typedefs.build())
                .enums(enums.build())
                .services(services.build());

        ImmutableList.Builder<StructElement> structElements = ImmutableList.builder();
        ImmutableList.Builder<StructElement> unionElements = ImmutableList.builder();
        ImmutableList.Builder<StructElement> exceptionElements = ImmutableList.builder();
        for (StructElement element : structs.build()) {
            switch (element.type()) {
                case STRUCT:
                    structElements.add(element);
                    break;

                case UNION:
                    unionElements.add(element);
                    break;

                case EXCEPTION:
                    exceptionElements.add(element);
                    break;

                default:
                    throw new AssertionError("Unexpected struct type: " + element.type());
            }
        }

        return builder
                .structs(structElements.build())
                .unions(unionElements.build())
                .exceptions(exceptionElements.build())
                .build();
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
            if (scope == null) {
                throw unexpected("invalid namespace scope: " + scopeName);
            }
            if (scope == NamespaceScope.PHP) {
                throw unexpected("scoped namespaces for PHP are not supported");
            }

            String namespace = readWord();
            return NamespaceElement.builder(location)
                    .scope(scope)
                    .namespace(namespace)
                    .build();
        }

        if ("php_namespace".equals(word)) {
            String namespace = readWord();
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
            throw unexpected("const is not yet implemented");
        }

        if ("typedef".equals(word)) {
            ++declCount;
            String oldType = readTypeName();
            String newName = readWord();

            return TypedefElement.builder(location)
                    .documentation(doc)
                    .oldName(oldType)
                    .newName(newName)
                    .build();
        }

        if ("senum".equals(word)) {
            throw unexpected("senum has been deprecated and is not supported.");
        }

        if ("struct".equals(word)) {
            throw unexpected("struct is not yet implemented");
        }

        if ("union".equals(word)) {
            throw unexpected("union is not yet implemented");
        }

        if ("exception".equals(word)) {
            throw unexpected("exception is not yet implemented");
        }

        if ("service".equals(word)) {
            throw unexpected("service is not yet implemented");
        }

        throw unexpected("unexpected element: " + word);
    }

    private char readChar() {
        char c = peekChar();
        pos++;
        return c;
    }

    private char peekChar() {
        skipWhitespace(true);
        if (pos == data.length) {
            throw unexpected("unexpected end of file");
        }
        return data[pos];
    }

    private String readLiteral() {
        char quote = peekChar();
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
                        throw unexpected("invalid escape character: " + escape);
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

    private String readTypeName() {
        String name = readWord();
        if ("list".equals(name) || "set".equals(name)) {
            if (readChar() != '<') {
                throw unexpected("missing type parameter");
            }
            String param = readTypeName();
            if (readChar() != '>') {
                throw unexpected("missing closing '>' in parameter list");
            }
            return name + "<" + param + ">";
        }

        if ("map".equals(name)) {
            if (readChar() != '<') {
                throw unexpected("missing type parameter list");
            }

            String keyType = readTypeName();
            if (readChar() != ',') {
                throw unexpected("invalid map-type parameter list");
            }

            String valueType = readTypeName();
            if (readChar() != '>') {
                throw unexpected("missing closing '>' in parameter list");
            }

            return "map<" + keyType + "," + valueType + ">";
        }

        return name;
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
        String word = readWord();
        try {
            int base = 10;
            if (word.startsWith("0x") || word.startsWith("0X")) {
                word = word.substring(2);
                base = 16;
            }
            return Integer.valueOf(word, base);
        } catch (Exception e) {
            throw unexpected("expected an int but was " + word);
        }
    }

    private String readDocumentation() {
        String result = null;
        while (true) {
            skipWhitespace(false);
            if (pos == data.length || (data[pos] != '/' && data[pos] != '#')) {
                return result != null ? result : "";
            }
            String comment = readComment();
            result = result == null ? comment : (result + "\n" + comment);
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
