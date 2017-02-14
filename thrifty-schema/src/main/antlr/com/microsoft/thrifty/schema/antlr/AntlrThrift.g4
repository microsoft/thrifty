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
grammar AntlrThrift;

@header {
  package com.microsoft.thrifty.schema.antlr;
}

document: header* definition*;

header
    : include
    | cppInclude
    | namespace
    ;

include
    : 'include' UNESCAPED_LITERAL // unescaped to preserve Windows file paths
    ;

cppInclude
    : 'cpp_include' UNESCAPED_LITERAL // unescaped to preserve Windows file paths
    ;

namespace
    : standard_namespace
    | php_namespace
    | xsd_namespace
    ;

standard_namespace
    : 'namespace' scope=IDENTIFIER ns=IDENTIFIER annotationList? SEPARATOR?
    ;

php_namespace
    : 'php_namespace' ns=LITERAL annotationList? SEPARATOR?
    ;

xsd_namespace
    : 'xsd_namespace' ns=LITERAL annotationList? SEPARATOR?
    ;

definition
    : constDef
    | typedef
    | enumDef
    | senum
    | structDef
    | unionDef
    | exceptionDef
    | serviceDef
    ;

// prefixed because 'const' is a reserved keyword in Java
constDef
    : 'const' fieldType IDENTIFIER '=' constValue SEPARATOR?
    ;

constValue
    : INTEGER
    | DOUBLE
    | LITERAL
    | IDENTIFIER
    | constList
    | constMap
    ;

constList
    : '[' (constList SEPARATOR?)* ']'
    ;

constMap
    : '{' (constValue ':' constValue SEPARATOR?)* '}'
    ;

typedef
    : 'typedef' fieldType IDENTIFIER SEPARATOR? annotationList?
    ;

// prefixed because 'enum' is a reserved keyword in Java
enumDef
    : 'enum' IDENTIFIER '{' enum_member* '}' annotationList?
    ;

enum_member
    : IDENTIFIER ('=' INTEGER)? SEPARATOR? annotationList?
    ;

senum
    : 'senum' IDENTIFIER '{' enum_member* '}' { System.err.println("WARNING: 'senum' is deprecated and unsupported!"); }
    ;

structDef
    : 'struct' IDENTIFIER '{' field* '}'
    ;

unionDef
    : 'union' IDENTIFIER '{' field* '}'
    ;

exceptionDef
    : 'exception' IDENTIFIER '{' field* '}'
    ;

serviceDef
    : 'service' IDENTIFIER '{' function* '}'
    ;

function
    : 'oneway'? ('void' | fieldType) IDENTIFIER fieldList throwsList? SEPARATOR?
    ;

fieldList
    : '(' field* ')'
    ;

field
    : NATURAL_INTEGER ':' ('required' | 'optional')? fieldType IDENTIFIER ('=' constDef)? SEPARATOR?
    ;

throwsList
    : 'throws' fieldList
    ;

fieldType
    : baseType annotationList?
    | containerType annotationList?
    | IDENTIFIER annotationList?
    ;

baseType
    : 'bool'
    | 'byte'
    | 'i8'
    | 'i16'
    | 'i32'
    | 'i64'
    | 'double'
    | 'string'
    | 'binary'
    | 'slist' { System.err.println("WARNING: 'slist' is deprecated and should be replaced with 'string'"); }
    ;

containerType
    : mapType
    | setType
    | listType
    ;

mapType
    : 'map' cppType? '<' key=fieldType COMMA value=fieldType '>'
    ;

listType
    : 'list' '<' fieldType '>' cppType?
    ;

setType
    : 'set' cppType? '<' fieldType '>'
    ;

cppType
    : 'cpp_type' LITERAL
    ;

annotationList
    : '(' annotation* ')'
    ;

annotation
    : IDENTIFIER ('=' LITERAL)?
    ;

LITERAL
    : DOUBLE_QUOTE_LITERAL
    | SINGLE_QUOTE_LITERAL
    ;

fragment DOUBLE_QUOTE_LITERAL
    : '"' (ESCAPE_CHAR|'\\"'|~'"')* '"'
    ;

fragment SINGLE_QUOTE_LITERAL
    : '\'' (ESCAPE_CHAR|'\\\''|~'\'')* '\''
    ;

UNESCAPED_LITERAL
    : '"' (~["])*? '"'
    | '\'' (~['])*? '\''
    ;

fragment ESCAPE_CHAR
    : '\\' [0\\btnr]
    | '\\u' HEX HEX HEX HEX
    ;

SEPARATOR
    : COMMA
    | SEMICOLON
    ;

COMMA
    : ','
    ;

SEMICOLON
    : ';'
    ;

IDENTIFIER
    : ID_START_CHAR ID_CHAR*
    ;

fragment ID_START_CHAR : [_a-zA-Z] ;

fragment ID_CHAR : [_a-zA-Z0-9.] ;

fragment HEX
    : [a-fA-F0-9]
    ;

NATURAL_INTEGER
    : [1-9] [0-9]+
    ;

INTEGER
    : [+\-]? INT
    | [+\-]? '0' [Xx] [0-9a-fA-F]+
    ;

DOUBLE
    : [+\-]? INT '.' [0-9]+ EXP?
    | [+\-]? INT EXP
    | [+\-]? INT
    ;

fragment INT
    : '0'
    | [1-9] [0-9]* // no leading zeroes
    ;

fragment EXP
    : [Ee] [+\-]? INT
    ;

SLASH_SLASH_COMMENT
    : '//' ~[\r\n]*? NL -> channel(HIDDEN)
    ;

HASH_COMMENT
    : '#' ~[\r\n]* NL -> channel(HIDDEN)
    ;

MULTILINE_COMMENT
    : '/*' .*? '*/' -> channel(HIDDEN)
    ;

NEWLINE
    : NL -> channel(HIDDEN)
    ;

fragment NL
    : '\r'? '\n'
    ;

WHITESPACE
    : [ \t] -> skip
    ;
