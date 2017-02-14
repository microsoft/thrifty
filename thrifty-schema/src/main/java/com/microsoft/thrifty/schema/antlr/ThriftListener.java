package com.microsoft.thrifty.schema.antlr;

import com.microsoft.thrifty.schema.ErrorReporter;
import com.microsoft.thrifty.schema.Location;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import com.microsoft.thrifty.schema.parser.IncludeElement;
import com.microsoft.thrifty.schema.parser.NamespaceElement;
import com.microsoft.thrifty.schema.parser.ThriftFileElement;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ThriftListener extends AntlrThriftBaseListener {
    private final CommonTokenStream tokenStream;
    private final ErrorReporter errorReporter;
    private final Location location;

    // We need to record which comment tokens have been treated as trailing documentation,
    // so that scanning for leading doc tokens for subsequent elements knows where to stop.
    // We can do this with a bitset tracking token indices of trailing-comment tokens.
    private final BitSet trailingDocTokenIndexes = new BitSet();

    private final List<IncludeElement> includes = new ArrayList<>();
    private final List<NamespaceElement> namespaces = new ArrayList<>();

    ThriftListener(CommonTokenStream tokenStream, ErrorReporter errorReporter, Location location) {
        this.tokenStream = tokenStream;
        this.errorReporter = errorReporter;
        this.location = location;
    }

    @Override
    public void enterDocument(AntlrThriftParser.DocumentContext ctx) {
        namespaces.clear();
    }

    @Override
    public void exitDocument(AntlrThriftParser.DocumentContext ctx) {
        super.exitDocument(ctx);
    }

    @Override
    public void exitInclude(AntlrThriftParser.IncludeContext ctx) {
        TerminalNode pathNode = ctx.UNESCAPED_LITERAL();
        String path = unquote(locationOf(pathNode), pathNode.getText(), false);

        includes.add(IncludeElement.create(locationOf(ctx), false, path));
    }

    @Override
    public void exitCppInclude(AntlrThriftParser.CppIncludeContext ctx) {
        TerminalNode pathNode = ctx.UNESCAPED_LITERAL();
        String path = unquote(locationOf(pathNode), pathNode.getText(), false);

        includes.add(IncludeElement.create(locationOf(ctx), true, path));
    }

    @Override
    public void exitStandard_namespace(AntlrThriftParser.Standard_namespaceContext ctx) {
        String scopeName = ctx.scope.getText();
        String name = ctx.ns.getText();

        AnnotationElement annotations = fromAntlr(ctx.annotationList());

        NamespaceScope scope = NamespaceScope.forThriftName(scopeName);
        if (scope == null) {
            errorReporter.warn(locationOf(ctx.scope), "Unknown namespace scope '" + scopeName + "'");
            scope = NamespaceScope.UNKNOWN;
        }

        NamespaceElement element = NamespaceElement.builder(locationOf(ctx))
                .scope(scope)
                .namespace(name)
                .annotations(annotations)
                .build();

        namespaces.add(element);
    }

    @Override
    public void exitPhp_namespace(AntlrThriftParser.Php_namespaceContext ctx) {
        NamespaceElement element = NamespaceElement.builder(locationOf(ctx))
                .scope(NamespaceScope.PHP)
                .namespace(ctx.LITERAL().getText())
                .annotations(fromAntlr(ctx.annotationList()))
                .build();

        namespaces.add(element);
    }

    @Override
    public void exitXsd_namespace(AntlrThriftParser.Xsd_namespaceContext ctx) {
        errorReporter.error(locationOf(ctx), "'xsd_namespace' is unsupported");
    }

    @Override
    public void exitT_enum(AntlrThriftParser.T_enumContext ctx) {
        String enumName = ctx.IDENTIFIER().getText();

        int nextValue = 0;
        Set<Integer> values = new HashSet<>();

        List<EnumMemberElement> members = new ArrayList<>(ctx.enum_member().size());
        for (AntlrThriftParser.Enum_memberContext memberContext : ctx.enum_member()) {
            List<Token> docComments = new ArrayList<>();
            docComments.addAll(getLeadingComments(memberContext.getStart()));
            docComments.addAll(getTrailingComments(memberContext.getStop()));

            String doc = formatJavadoc(docComments);

            int value = nextValue;
            TerminalNode valueToken = memberContext.INTEGER();
            if (valueToken != null) {
                value = parseInt(valueToken);
            }

            if (!values.add(value)) {
                errorReporter.error(locationOf(valueToken), "duplicate enum value: " + value);
                continue;
            }

            nextValue = value + 1;

            EnumMemberElement element = EnumMemberElement.builder(locationOf(memberContext))
                    .name(memberContext.IDENTIFIER().getText())
                    .value(value)
                    .documentation(doc)
                    .annotations(fromAntlr(memberContext.annotationList()))
                    .build();

            members.add(element);
        }
    }

    // region Utilities

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

    private AnnotationElement fromAntlr(AntlrThriftParser.AnnotationListContext ctx) {
        if (ctx == null) {
            return null;
        }

        Map<String, String> annotations = new LinkedHashMap<>();
        for (AntlrThriftParser.AnnotationContext annotationContext : ctx.annotation()) {
            String name = annotationContext.IDENTIFIER().getText();
            String value;
            if (annotationContext.LITERAL() != null) {
                value = annotationContext.LITERAL().getText();
            } else {
                value = "";
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
        int col = token.getCharPositionInLine();
        return location.at(line, col);
    }

    private String unquote(Location location, String literal) {
        return unquote(location, literal, /* processEscapes */ true);
    }

    private String unquote(Location location, String literal, boolean processEscapes) {
        char[] chars = literal.toCharArray();
        char startChar = chars[0];
        char endChar = chars[chars.length - 1];

        if (startChar != endChar || (startChar != '\'' && startChar != '"') || (endChar != '\'' && endChar != '"')) {
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

    private static String formatJavadoc(List<Token> commentTokens) {
        StringBuilder sb = new StringBuilder();

        for (Token token : commentTokens) {
            String text = token.getText();
            int commentStart;
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

        return sb.toString();
    }

    private static void formatSingleLineComment(StringBuilder sb, String text, String prefix) {
        int start = prefix.length();
        int end = text.length();

        while (Character.isWhitespace( text.charAt(start) )) {
            ++start;
        }

        while (Character.isWhitespace( text.charAt(end - 1) )) {
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
                sb.append(c);

                // skip a single subsequent space, if it exists
                if (Character.isWhitespace( chars[pos + 1] )) {
                    ++pos;
                }

                isStartOfLine = false;
            } else if (! Character.isWhitespace( c )) {
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