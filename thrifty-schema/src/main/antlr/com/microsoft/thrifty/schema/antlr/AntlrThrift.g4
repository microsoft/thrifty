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

document: header* definition*;

header
    : include
    | cppInclude
    | namespace
    ;

include
    : 'include' LITERAL separator?
    ;

cppInclude
    : 'cpp_include' LITERAL separator?
    ;

namespace
    : standardNamespace
    | phpNamespace
    | xsdNamespace
    ;

standardNamespace
    : 'namespace' namespaceScope ns=IDENTIFIER annotationList? separator?
    ;

namespaceScope
    : '*'
    | IDENTIFIER
    ;

phpNamespace
    : 'php_namespace' ns=LITERAL annotationList? separator?
    ;

xsdNamespace
    : 'xsd_namespace' ns=LITERAL annotationList? separator?
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

constDef
    : 'const' fieldType IDENTIFIER '=' constValue separator?
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
    : '[' (constValue separator?)* ']'
    ;

constMap
    : '{' (constMapEntry separator?)* '}'
    ;

constMapEntry
    : key=constValue ':' value=constValue
    ;

typedef
    : 'typedef' fieldType IDENTIFIER annotationList? separator?
    ;

enumDef
    : 'enum' IDENTIFIER '{' enumMember* '}' annotationList?
    ;

enumMember
    : IDENTIFIER ('=' INTEGER)? annotationList? separator?
    ;

senum
    : 'senum' IDENTIFIER '{' enumMember* '}'
    ;

structDef
    : 'struct' IDENTIFIER '{' field* '}' annotationList?
    ;

unionDef
    : 'union' IDENTIFIER '{' field* '}' annotationList?
    ;

exceptionDef
    : 'exception' IDENTIFIER '{' field* '}' annotationList?
    ;

serviceDef
    : 'service' name=IDENTIFIER ('extends' superType=fieldType)? '{' function* '}' annotationList?
    ;

function
    : ONEWAY? (VOID | fieldType) IDENTIFIER fieldList throwsList? annotationList? separator?
    ;

fieldList
    : '(' field* ')'
    ;

field
    : (INTEGER ':')? requiredness? fieldType IDENTIFIER ('=' constValue)? annotationList? separator?
    ;

requiredness
    : 'optional'
    | 'required'
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
    | 'slist'
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
    : IDENTIFIER ('=' LITERAL)? separator?
    ;

separator
    : COMMA
    | SEMICOLON
    ;

LITERAL
    : DOUBLE_QUOTE_LITERAL
    | SINGLE_QUOTE_LITERAL
    ;

fragment DOUBLE_QUOTE_LITERAL
    : '"' ( '\\"' | (~["\r\n]))* '"'
    ;

fragment SINGLE_QUOTE_LITERAL
    : '\'' ( '\\\'' | ~['\r\n])* '\''
    ;

fragment ESCAPE_CHAR
    : '\\' [0\\btnr]
    | '\\u' HEX HEX HEX HEX
    ;

VOID
    : 'void'
    ;

ONEWAY
    : 'oneway'
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

NS_SCOPE
    : '*'
    | IDENTIFIER
    ;

fragment ID_START_CHAR : [_a-zA-Z] ;

fragment ID_CHAR : [_a-zA-Z0-9.] ;

fragment HEX
    : [a-fA-F0-9]
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
