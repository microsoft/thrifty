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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.ErrorReporter;
import com.microsoft.thrifty.schema.Location;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.Requiredness;
import com.microsoft.thrifty.schema.antlr.AntlrThriftBaseListener;
import com.microsoft.thrifty.schema.antlr.AntlrThriftLexer;
import com.microsoft.thrifty.schema.antlr.AntlrThriftParser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of callbacks that, when used with a {@link org.antlr.v4.runtime.tree.ParseTreeWalker},
 * assemble a {@link ThriftFileElement} from an {@link AntlrThriftParser}.
 *
 * Instances of this class are single-use; after walking a parse tree, it will contain
 * that parser's state as thrifty-schema parser elements.
 */
class ThriftListener extends AntlrThriftBaseListener {
    // A number of tokens that should comfortably accommodate most input files
    // without wildly re-allocating.  Estimated based on the ClientTestThrift
    // and TestThrift files, which contain around ~1200 tokens each.
    private static final int INITIAL_BITSET_CAPACITY = 2048;

    private final CommonTokenStream tokenStream;
    private final ErrorReporter errorReporter;
    private final Location location;

    // We need to record which comment tokens have been treated as trailing documentation,
    // so that scanning for leading doc tokens for subsequent elements knows where to stop.
    // We can do this with a bitset tracking token indices of trailing-comment tokens.
    private final BitSet trailingDocTokenIndexes = new BitSet(INITIAL_BITSET_CAPACITY);

    private final List<IncludeElement> includes = new ArrayList<>();
    private final List<NamespaceElement> namespaces = new ArrayList<>();
    private final List<EnumElement> enums = new ArrayList<>();
    private final List<TypedefElement> typedefs = new ArrayList<>();
    private final List<StructElement> structs = new ArrayList<>();
    private final List<StructElement> unions = new ArrayList<>();
    private final List<StructElement> exceptions = new ArrayList<>();
    private final List<ConstElement> consts = new ArrayList<>();
    private final List<ServiceElement> services = new ArrayList<>();

    /**
     * Creates a new ThriftListener instance.
     *
     * @param tokenStream the same token stream used with the corresponding {@link AntlrThriftParser};
     *                    this stream will be queried for "hidden" tokens containing parsed doc
     *                    comments.
     * @param errorReporter an error reporting mechanism, used to communicate errors during parsing.
     * @param location a location pointing at the beginning of the file being parsed.
     */
    ThriftListener(CommonTokenStream tokenStream, ErrorReporter errorReporter, Location location) {
        this.tokenStream = tokenStream;
        this.errorReporter = errorReporter;
        this.location = location;
    }

    ThriftFileElement buildFileElement() {
        return ThriftFileElement.builder(location)
                .includes(includes)
                .namespaces(namespaces)
                .typedefs(typedefs)
                .enums(enums)
                .structs(structs)
                .unions(unions)
                .exceptions(exceptions)
                .constants(consts)
                .services(services)
                .build();
    }

    @Override
    public void exitInclude(AntlrThriftParser.IncludeContext ctx) {
        TerminalNode pathNode = ctx.LITERAL();
        String path = unquote(locationOf(pathNode), pathNode.getText(), false);

        includes.add(IncludeElement.create(locationOf(ctx), false, path));
    }

    @Override
    public void exitCppInclude(AntlrThriftParser.CppIncludeContext ctx) {
        TerminalNode pathNode = ctx.LITERAL();
        String path = unquote(locationOf(pathNode), pathNode.getText(), false);

        includes.add(IncludeElement.create(locationOf(ctx), true, path));
    }

    @Override
    public void exitStandardNamespace(AntlrThriftParser.StandardNamespaceContext ctx) {
        String scopeName = ctx.namespaceScope().getText();
        String name = ctx.ns.getText();

        AnnotationElement annotations = annotationsFromAntlr(ctx.annotationList());

        NamespaceScope scope = NamespaceScope.forThriftName(scopeName);
        if (scope == null) {
            errorReporter.warn(locationOf(ctx.namespaceScope()), "Unknown namespace scope '" + scopeName + "'");
            return;
        }

        NamespaceElement element = NamespaceElement.builder(locationOf(ctx))
                .scope(scope)
                .namespace(name)
                .annotations(annotations)
                .build();

        namespaces.add(element);
    }

    @Override
    public void exitPhpNamespace(AntlrThriftParser.PhpNamespaceContext ctx) {
        NamespaceElement element = NamespaceElement.builder(locationOf(ctx))
                .scope(NamespaceScope.PHP)
                .namespace(unquote(locationOf(ctx.LITERAL()), ctx.LITERAL().getText()))
                .annotations(annotationsFromAntlr(ctx.annotationList()))
                .build();

        namespaces.add(element);
    }

    @Override
    public void exitXsdNamespace(AntlrThriftParser.XsdNamespaceContext ctx) {
        errorReporter.error(locationOf(ctx), "'xsd_namespace' is unsupported");
    }

    @Override
    public void exitSenum(AntlrThriftParser.SenumContext ctx) {
        errorReporter.error(locationOf(ctx), "'senum' is unsupported; use 'enum' instead");
    }

    @Override
    public void exitEnumDef(AntlrThriftParser.EnumDefContext ctx) {
        String enumName = ctx.IDENTIFIER().getText();

        int nextValue = 0;
        Set<Integer> values = new HashSet<>();

        List<EnumMemberElement> members = new ArrayList<>(ctx.enumMember().size());
        for (AntlrThriftParser.EnumMemberContext memberContext : ctx.enumMember()) {
            int value = nextValue;

            TerminalNode valueToken = memberContext.INTEGER();
            if (valueToken != null) {
                value = parseInt(valueToken);
            }

            if (!values.add(value)) {
                errorReporter.error(locationOf(memberContext), "duplicate enum value: " + value);
                continue;
            }

            nextValue = value + 1;

            EnumMemberElement element = EnumMemberElement.builder(locationOf(memberContext))
                    .name(memberContext.IDENTIFIER().getText())
                    .value(value)
                    .documentation(formatJavadoc(memberContext))
                    .annotations(annotationsFromAntlr(memberContext.annotationList()))
                    .build();

            members.add(element);
        }

        String doc = formatJavadoc(ctx);
        EnumElement element = EnumElement.builder(locationOf(ctx))
                .name(enumName)
                .documentation(doc)
                .annotations(annotationsFromAntlr(ctx.annotationList()))
                .members(members)
                .build();

        enums.add(element);
    }

    @Override
    public void exitStructDef(AntlrThriftParser.StructDefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        ImmutableList<FieldElement> fields = parseFieldList(ctx.field());

        StructElement element = StructElement.builder(locationOf(ctx))
                .name(name)
                .fields(fields)
                .type(StructElement.Type.STRUCT)
                .documentation(formatJavadoc(ctx))
                .annotations(annotationsFromAntlr(ctx.annotationList()))
                .build();

        structs.add(element);
    }

    @Override
    public void exitUnionDef(AntlrThriftParser.UnionDefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        ImmutableList<FieldElement> fields = parseFieldList(ctx.field());

        int numFieldsWithDefaultValues = 0;
        for (int i = 0; i < fields.size(); ++i) {
            FieldElement element = fields.get(i);
            if (element.requiredness() == Requiredness.REQUIRED) {
                AntlrThriftParser.FieldContext fieldContext = ctx.field(i);
                errorReporter.error(locationOf(fieldContext), "unions cannot have required fields");
            }

            if (element.constValue() != null) {
                ++numFieldsWithDefaultValues;
            }
        }

        if (numFieldsWithDefaultValues > 1) {
            errorReporter.error(locationOf(ctx), "unions can have at most one default value");
        }

        StructElement element = StructElement.builder(locationOf(ctx))
                .name(name)
                .fields(fields)
                .type(StructElement.Type.UNION)
                .documentation(formatJavadoc(ctx))
                .annotations(annotationsFromAntlr(ctx.annotationList()))
                .build();

        unions.add(element);
    }

    @Override
    public void exitExceptionDef(AntlrThriftParser.ExceptionDefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        ImmutableList<FieldElement> fields = parseFieldList(ctx.field());

        StructElement element = StructElement.builder(locationOf(ctx))
                .name(name)
                .fields(fields)
                .type(StructElement.Type.EXCEPTION)
                .documentation(formatJavadoc(ctx))
                .annotations(annotationsFromAntlr(ctx.annotationList()))
                .build();

        exceptions.add(element);
    }

    private ImmutableList<FieldElement> parseFieldList(List<AntlrThriftParser.FieldContext> contexts) {
        return parseFieldList(contexts, Requiredness.DEFAULT);
    }

    private ImmutableList<FieldElement> parseFieldList(
            List<AntlrThriftParser.FieldContext> contexts,
            Requiredness defaultRequiredness) {
        ImmutableList.Builder<FieldElement> builder = ImmutableList.builder();
        Set<Integer> ids = new HashSet<>();

        int nextValue = 1;
        for (AntlrThriftParser.FieldContext fieldContext : contexts) {
            FieldElement element = parseField(nextValue, fieldContext, defaultRequiredness);
            if (element != null) {
                builder = builder.add(element);

                if (!ids.add(element.fieldId())) {
                    errorReporter.error(locationOf(fieldContext), "duplicate field ID: " + element.fieldId());
                }

                if (element.fieldId() <= 0) {
                    errorReporter.error(locationOf(fieldContext), "field ID must be greater than zero");
                }

                if (element.fieldId() >= nextValue) {
                    nextValue = element.fieldId() + 1;
                }
            } else {
                // assert-fail here?
                ++nextValue; // this represents an error condition
            }
        }

        return builder.build();
    }

    private FieldElement parseField(
            int defaultValue,
            AntlrThriftParser.FieldContext ctx,
            Requiredness defaultRequiredness) {
        int fieldId = defaultValue;
        if (ctx.INTEGER() != null) {
            fieldId = parseInt(ctx.INTEGER());
        }

        String fieldName = ctx.IDENTIFIER().getText();

        Requiredness requiredness = defaultRequiredness;
        if (ctx.requiredness() != null) {
            if (ctx.requiredness().getText().equals("required")) {
                requiredness = Requiredness.REQUIRED;
            } else if (ctx.requiredness().getText().equals("optional")) {
                requiredness = Requiredness.OPTIONAL;
            } else {
                throw new AssertionError("Unexpected requiredness value: " + ctx.requiredness().getText());
            }
        }

        return FieldElement.builder(locationOf(ctx))
                .documentation(formatJavadoc(ctx))
                .fieldId(fieldId)
                .requiredness(requiredness)
                .type(typeElementOf(ctx.fieldType()))
                .name(fieldName)
                .annotations(annotationsFromAntlr(ctx.annotationList()))
                .constValue(constValueElementOf(ctx.constValue()))
                .build();
    }

    @Override
    public void exitTypedef(AntlrThriftParser.TypedefContext ctx) {
        TypeElement oldType = typeElementOf(ctx.fieldType());

        TypedefElement typedef = TypedefElement.builder(locationOf(ctx))
                .documentation(formatJavadoc(ctx))
                .annotations(annotationsFromAntlr(ctx.annotationList()))
                .oldType(oldType)
                .newName(ctx.IDENTIFIER().getText())
                .build();

        typedefs.add(typedef);
    }

    @Override
    public void exitConstDef(AntlrThriftParser.ConstDefContext ctx) {
        ConstElement element = ConstElement.builder(locationOf(ctx))
                .documentation(formatJavadoc(ctx))
                .type(typeElementOf(ctx.fieldType()))
                .name(ctx.IDENTIFIER().getText())
                .value(constValueElementOf(ctx.constValue()))
                .build();

        consts.add(element);
    }

    @Override
    public void exitServiceDef(AntlrThriftParser.ServiceDefContext ctx) {
        String name = ctx.name.getText();

        ServiceElement.Builder builder = ServiceElement.builder(locationOf(ctx))
                .name(name)
                .functions(parseFunctionList(ctx.function()))
                .documentation(formatJavadoc(ctx))
                .annotations(annotationsFromAntlr(ctx.annotationList()));

        if (ctx.superType != null) {
            TypeElement superType = typeElementOf(ctx.superType);

            if (!(superType instanceof ScalarTypeElement)) {
                errorReporter.error(locationOf(ctx), "services cannot extend collections");
            }

            builder = builder.extendsService(superType);
        }

        services.add(builder.build());
    }

    private ImmutableList<FunctionElement> parseFunctionList(List<AntlrThriftParser.FunctionContext> functionContexts) {
        ImmutableList.Builder<FunctionElement> functions = ImmutableList.builder();

        for (AntlrThriftParser.FunctionContext ctx : functionContexts) {
            String name = ctx.IDENTIFIER().getText();

            TypeElement returnType;
            if (ctx.fieldType() != null) {
                returnType = typeElementOf(ctx.fieldType());
            } else {
                TerminalNode token = ctx.getToken(AntlrThriftLexer.VOID, 0);
                if (token == null) {
                    throw new AssertionError("Function has no return type, and no VOID token - grammar error");
                }
                Location loc = locationOf(token);

                // Do people actually annotation 'void'?  We'll find out!
                returnType = TypeElement.scalar(loc, "void", null);
            }

            boolean isOneway = ctx.ONEWAY() != null;

            FunctionElement.Builder builder = FunctionElement.builder(locationOf(ctx))
                    .oneWay(isOneway)
                    .returnType(returnType)
                    .name(name)
                    .documentation(formatJavadoc(ctx))
                    .annotations(annotationsFromAntlr(ctx.annotationList()))
                    .params(parseFieldList(ctx.fieldList().field(), Requiredness.REQUIRED));

            if (ctx.throwsList() != null) {
                builder = builder.exceptions(parseFieldList(ctx.throwsList().fieldList().field()));
            }

            functions.add(builder.build());
        }

        return functions.build();
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        errorReporter.error(locationOf(node), node.getText());
    }

    // region Utilities

    private AnnotationElement annotationsFromAntlr(AntlrThriftParser.AnnotationListContext ctx) {
        if (ctx == null) {
            return null;
        }

        Map<String, String> annotations = new LinkedHashMap<>();
        for (AntlrThriftParser.AnnotationContext annotationContext : ctx.annotation()) {
            String name = annotationContext.IDENTIFIER().getText();
            String value;
            if (annotationContext.LITERAL() != null) {
                value = unquote(locationOf(annotationContext.LITERAL()), annotationContext.LITERAL().getText());
            } else {
                value = "true";
            }
            annotations.put(name, value);
        }

        return AnnotationElement.create(locationOf(ctx), annotations);
    }

    private Location locationOf(ParserRuleContext ctx) {
        return locationOf(ctx.getStart());
    }

    private Location locationOf(TerminalNode node) {
        return locationOf(node.getSymbol());
    }

    private Location locationOf(Token token) {
        int line = token.getLine();
        int col = token.getCharPositionInLine() + 1; // Location.col is 1-based, Token.col is 0-based
        return location.at(line, col);
    }

    private String unquote(Location location, String literal) {
        return unquote(location, literal, /* processEscapes */ true);
    }

    private String unquote(Location location, String literal, boolean processEscapes) {
        char[] chars = literal.toCharArray();
        char startChar = chars[0];
        char endChar = chars[chars.length - 1];

        if (startChar != endChar || (startChar != '\'' && startChar != '"')) {
            throw new AssertionError("Incorrect UNESCAPED_LITERAL rule: " + literal);
        }

        StringBuilder sb = new StringBuilder(literal.length() - 2);

        int i = 1;
        int end = chars.length - 1;
        while (i < end) {
            char c = chars[i++];

            if (processEscapes && c == '\\') {
                if (i == end) {
                    errorReporter.error(location, "Unterminated literal");
                    break;
                }

                char escape = chars[i++];
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
                    case 'u':
                        throw new UnsupportedOperationException("unicode escapes not yet implemented");
                    default:
                        if (escape == startChar) {
                            sb.append(startChar);
                        } else {
                            errorReporter.error(location, "invalid escape character: " + escape);
                        }
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private TypeElement typeElementOf(AntlrThriftParser.FieldTypeContext context) {
        if (context.baseType() != null) {
            if (context.baseType().getText().equals("slist")) {
                errorReporter.error(locationOf(context), "slist is unsupported; use list<string> instead");
            }

            return TypeElement.scalar(
                    locationOf(context),
                    context.baseType().getText(),
                    annotationsFromAntlr(context.annotationList()));
        }

        if (context.IDENTIFIER() != null) {
            return TypeElement.scalar(
                    locationOf(context),
                    context.IDENTIFIER().getText(),
                    annotationsFromAntlr(context.annotationList()));
        }

        if (context.containerType() != null) {
            AntlrThriftParser.ContainerTypeContext containerContext = context.containerType();
            if (containerContext.mapType() != null) {
                TypeElement keyType = typeElementOf(containerContext.mapType().key);
                TypeElement valueType = typeElementOf(containerContext.mapType().value);
                return TypeElement.map(
                        locationOf(containerContext.mapType()),
                        keyType,
                        valueType,
                        annotationsFromAntlr(context.annotationList()));
            }

            if (containerContext.setType() != null) {
                return TypeElement.set(
                        locationOf(containerContext.setType()),
                        typeElementOf(containerContext.setType().fieldType()),
                        annotationsFromAntlr(context.annotationList()));
            }

            if (containerContext.listType() != null) {
                return TypeElement.list(
                        locationOf(containerContext.listType()),
                        typeElementOf(containerContext.listType().fieldType()),
                        annotationsFromAntlr(context.annotationList()));
            }

            throw new AssertionError("Unexpected container type - grammar error!");
        }

        throw new AssertionError("Unexpected type - grammar error!");
    }

    private ConstValueElement constValueElementOf(AntlrThriftParser.ConstValueContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.INTEGER() != null) {
            String text = ctx.INTEGER().getText();

            int radix = 10;
            if (text.startsWith("0x") || text.startsWith("0X")) {
                text = text.substring(2);
                radix = 16;
            }

            try {
                long value = Long.parseLong(text, radix);

                return ConstValueElement.integer(locationOf(ctx), ctx.INTEGER().getText(), value);
            } catch (NumberFormatException e) {
                throw new AssertionError("Invalid integer accepted by ANTLR grammar: " + ctx.INTEGER().getText());
            }
        }

        if (ctx.DOUBLE() != null) {
            String text = ctx.DOUBLE().getText();

            try {
                double value = Double.parseDouble(text);
                return ConstValueElement.real(locationOf(ctx), ctx.DOUBLE().getText(), value);
            } catch (NumberFormatException e) {
                throw new AssertionError("Invalid double accepted by ANTLR grammar: " + text);
            }
        }

        if (ctx.LITERAL() != null) {
            String text = unquote(locationOf(ctx.LITERAL()), ctx.LITERAL().getText());
            return ConstValueElement.literal(locationOf(ctx), ctx.LITERAL().getText(), text);
        }

        if (ctx.IDENTIFIER() != null) {
            String id = ctx.IDENTIFIER().getText();
            return ConstValueElement.identifier(locationOf(ctx), ctx.IDENTIFIER().getText(), id);
        }

        if (ctx.constList() != null) {
            ImmutableList.Builder<ConstValueElement> values = ImmutableList.builder();
            for (AntlrThriftParser.ConstValueContext valueContext : ctx.constList().constValue()) {
                values.add(constValueElementOf(valueContext));
            }
            return ConstValueElement.list(locationOf(ctx), ctx.constList().getText(), values.build());
        }

        if (ctx.constMap() != null) {
            ImmutableMap.Builder<ConstValueElement, ConstValueElement> values = ImmutableMap.builder();
            for (AntlrThriftParser.ConstMapEntryContext entry : ctx.constMap().constMapEntry()) {
                ConstValueElement key = constValueElementOf(entry.key);
                ConstValueElement value = constValueElementOf(entry.value);
                values.put(key, value);
            }
            return ConstValueElement.map(locationOf(ctx), ctx.constMap().getText(), values.build());
        }

        throw new AssertionError("unreachable");
    }

    private static boolean isComment(Token token) {
        switch (token.getType()) {
            case AntlrThriftLexer.SLASH_SLASH_COMMENT:
            case AntlrThriftLexer.HASH_COMMENT:
            case AntlrThriftLexer.MULTILINE_COMMENT:
                return true;

            default:
                return false;
        }
    }

    private String formatJavadoc(ParserRuleContext context) {
        List<Token> tokens = new ArrayList<>();
        tokens.addAll(getLeadingComments(context.getStart()));
        tokens.addAll(getTrailingComments(context.getStop()));

        return formatJavadoc(tokens);
    }

    private List<Token> getLeadingComments(Token token) {
        List<Token> hiddenTokens = tokenStream.getHiddenTokensToLeft(token.getTokenIndex(), Lexer.HIDDEN);

        if (hiddenTokens == null || hiddenTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<Token> comments = new ArrayList<>(hiddenTokens.size());
        for (Token hiddenToken : hiddenTokens) {
            if (isComment(hiddenToken) && !trailingDocTokenIndexes.get(hiddenToken.getTokenIndex())) {
                comments.add(hiddenToken);
            }
        }

        return comments;
    }

    /**
     * Read comments following the given token, until the first newline is encountered.
     *
     * INVARIANT:
     * Assumes that the parse tree is being walked top-down, left to right!
     *
     * Trailing-doc tokens are marked as such, so that subsequent searches for "leading"
     * doc don't grab tokens already used as "trailing" doc.  If the walk order is *not*
     * top-down, left-to-right, then the assumption underpinning the separation of leading
     * and trailing comments is broken.
     *
     * @param endToken the token from which to search for trailing comment tokens.
     * @return a list, possibly empty, of all trailing comment tokens.
     */
    private List<Token> getTrailingComments(Token endToken) {
        List<Token> hiddenTokens = tokenStream.getHiddenTokensToRight(endToken.getTokenIndex(), Lexer.HIDDEN);

        if (hiddenTokens == null || hiddenTokens.isEmpty()) {
            return Collections.emptyList();
        }

        Token maybeTrailingDoc = hiddenTokens.get(0); // only one trailing comment is possible

        if (isComment(maybeTrailingDoc)) {
            trailingDocTokenIndexes.set(maybeTrailingDoc.getTokenIndex());
            return Collections.singletonList(maybeTrailingDoc);
        }

        return Collections.emptyList();
    }

    private static String formatJavadoc(List<Token> commentTokens) {
        StringBuilder sb = new StringBuilder();

        for (Token token : commentTokens) {
            String text = token.getText();
            switch (token.getType()) {
                case AntlrThriftLexer.SLASH_SLASH_COMMENT:
                    formatSingleLineComment(sb, text, "//");
                    break;

                case AntlrThriftLexer.HASH_COMMENT:
                    formatSingleLineComment(sb, text, "#");
                    break;

                case AntlrThriftLexer.MULTILINE_COMMENT:
                    formatMultilineComment(sb, text);
                    break;

                default:
                    // wut
                    break;
            }
        }

        String doc = sb.toString().trim();

        if (!Strings.isNullOrEmpty(doc) && !doc.endsWith("\n")) {
            doc += "\n";
        }

        return doc;
    }

    private static void formatSingleLineComment(StringBuilder sb, String text, String prefix) {
        int start = prefix.length();
        int end = text.length();

        while (Character.isWhitespace(text.charAt(start))) {
            ++start;
        }

        while (Character.isWhitespace(text.charAt(end - 1))) {
            --end;
        }

        sb.append(text.substring(start, end));
        sb.append("\n");
    }

    private static void formatMultilineComment(StringBuilder sb, String text) {
        char[] chars = text.toCharArray();
        int pos = "/*".length();
        int length = chars.length;
        boolean isStartOfLine = true;

        for (; pos + 1 < length; ++pos) {
            char c = chars[pos];
            if (c == '*' && chars[pos + 1] == '/') {
                sb.append("\n");
                return;
            }

            if (c == '\n') {
                sb.append(c);
                isStartOfLine = true;
            } else if (!isStartOfLine) {
                sb.append(c);
            } else if (c == '*') {
                // skip a single subsequent space, if it exists
                if (chars[pos + 1] == ' ') {
                    pos += 1;
                }

                isStartOfLine = false;
            } else if (! Character.isWhitespace(c)) {
                sb.append(c);
                isStartOfLine = false;
            }
        }
    }

    private static int parseInt(TerminalNode node) {
        return parseInt(node.getSymbol());
    }

    private static int parseInt(Token token) {
        String text = token.getText();

        int radix = 10;
        if (text.startsWith("0x") || text.startsWith("0X")) {
            radix = 16;
            text = text.substring(2);
        }

        return Integer.parseInt(text, radix);
    }

    // endregion
}
