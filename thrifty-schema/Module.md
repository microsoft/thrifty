# Module thrifty-schema

The Thrifty compiler "front-end".

## Package com.bendb.thrifty.schema.parser

Contains a Thrift parser implementation.

The output of the parser is an untyped AST - types are not validated, typedefs are unresolved, etc.

## Package com.bendb.thrifty.schema

Contains the core Thrifty model of the Thrift language.  The `Loader` class is the primary
interface used external code; it will read .thrift files, parse, and validate them, returning
a `Schema` representing the contents of those files.
