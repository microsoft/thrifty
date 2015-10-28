grammar Thrift;

@header {
package com.bendb.thrifty;
}

document
    :   header* definition*;
    
header
    :   include
    |   cppInclude
    |   namespace;

include
    :   'include' literal;

cppInclude
    :   'cpp_include' literal;

namespace
    :   'namespace' namespaceScope identifier
    |   'namespace' 'smalltalk.category' stIdentifier
    |   'namespace' 'smalltalk.prefix' identifier
    |   'php_namespace' literal;

namespaceScope
    :   '*'
    |   'cpp'
    |   'java'
    |   'py'
    |   'perl'
    |   'rb'
    |   'cocoa'
    |   'csharp'
    ;

definition
    :   constant
    |   typedef
    |   enumeration
    |   senum
    |   struct
    |   union
    |   exception
    |   service
    ;

constant
    :   'const' fieldType identifier '=' constValue listSeparator?
    ;

typedef
    :   'typedef' definitionType identifier
    ;

enumeration
    :   'enum' identifier '{' (identifier ('=' intConstant)? listSeparator?)* '}'
    ;

senum
    :   'senum' identifier '{' (literal listSeparator?)* '}'
    ;

struct
    :   'struct' identifier '{' field* '}'
    ;

union
    :   'union' identifier '{' field* '}'
    ;

exception
    :   'exception' identifier '{' field* '}'
    ;

service
    :   'service' identifier ('extends' identifier)? '{' function* '}'
    ;

field
    :   fieldId? fieldReq? fieldType identifier
    ;

fieldId
    :   intConstant ':'
    ;

fieldReq
    :   'required'
    |   'optional'
    ;

function
    :   'oneway'? functionType identifier '(' field* ')' throwClause? listSeparator?
    ;

functionType
    :   fieldType
    |   'void'
    ;

throwClause
    :   'throws' '(' (field listSeparator?)* ')'
    ;

fieldType
    :   identifier
    |   baseType
    |   containerType
    ;

definitionType
    :   baseType
    |   containerType
    ;

baseType
    :   'bool'
    |   'byte'
    |   'i16'
    |   'i32'
    |   'i64'
    |   'double'
    |   'string'
    |   'binary'
    |   'slist'
    ;

containerType
    :   mapType
    |   setType
    |   listType
    ;

mapType
    :   'map' cppType? '<' fieldType ',' fieldType '>'
    ;

setType
    :   'set' cppType? '<' fieldType '>'
    ;

listType
    :   'list' '<' fieldType '>'
    ;

cppType
    :   'cpp_type' literal
    ;

//////////////

constValue
    :   intConstant
    |   doubleConstant
    |   literal
    |   identifier
    |   constList
    |   constMap
    ;

intConstant
    :   ('+' | '-')? DIGIT+
    ;

doubleConstant
    :   ('+' | '-')? DIGIT+ ('.' DIGIT+)? ( ('E' | 'e') intConstant)?
    ;

constList
    :   '[' (constValue listSeparator?)* ']'
    ;

constMap
    :   '{' (constValue ':' constValue listSeparator?) '}'
    ;

//////////////////

literal
    :   '"' ~('"')* '"'
    |   '\'' ~('\'')* '\''
    ;

identifier
    :   (LETTER | '_') (LETTER | DIGIT | '.' | '_')*
    ;

stIdentifier
    :   (LETTER | '_') (LETTER | DIGIT | '.' | '_' | '-')*
    ;

listSeparator
    :   ','
    |   ';'
    ;

LETTER : [a-zA-Z];

DIGIT : [0-9];
