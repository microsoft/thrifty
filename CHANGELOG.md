0.4.1 (released π 2017)
------------------
- #133: Fix IndexOutOfBoundsException parsing empty comments

0.4.0 (released 8 March 2017)
------------------
- #127: Add JaCoCo reports to the build
- #126: Update Gradle to 3.4.1m
- #124: Add ErrorProne checks to the build
- #121: Replace 'java' plugin with 'java-library-plugin'
- #119: Use java.nio.file.Path in the Loader API
- #117: Update Gradle to 3.4
- #116: Emit hex literals from Thrift as hex literals in Java
- #115: Start using the Stream API
- #114: Update Guava
- #110: Update compiler to Java 8
- #109: Suppress StringEquality and NumberEquality on gen'd equals() methods
- #108: Improve generated ThriftField annotations
- #107: Fix: throw ThriftException on reading unrecognized enum values
- #104: Rewrite parser with ANTLR

0.3.2 (released 11 February 2017)
------------------
- #98, #99, #100, #101, #102: Improvements to builder APIs in thrifty-schema (thanks, @hzsweers)
- #96: Add builders to collection ThriftTypes
- #90: Upgrade JavaPoet in thrifty-java-codegen to 1.8.0
- #88: Upgrade Okio in thrifty-runtime to 1.11.0

0.3.1 (released 13 November 2016)
------------------
- #82: Fix i64 constants greater than Integer.MAX_VALUE
- #78: Fix bug preventing string const values from generating correctly

0.3.0 (released 9 November 2016)
------------------
- #73: Breaking change: Massive refactor of `thrifty-schema`, unifying `ThriftType` with `Named`
- #74: Add `.withNamespaces` API for `ThriftType` (thanks @hzsweers)
- #72: Add namespaces for `TypedefType` (thanks @hzsweers)
- #71: Fix: Include `@Nullable` fields in `.equals()` (thanks @naturalwarren)
- #70: Behavior change: Allow typedefs to be used with `TypeResolver`
- #67: Fix: Improve validation of enum constants whose types are imported
- #65: Add `DecoratingProtocol` (thanks @gabrielittner)
- #62: Fix: Remove `name` from `Field#hashcode()`
- #61: Add builders for most `thrifty-schema` types (thanks @naturalwarren)
- #60: Fix: Const validation when a typedef is assigned an enum literal value
- #59: Fix: Allow constants and types with the same name
- #58: Behavior change: Obfuscated fields that are missing are printed as 'null'
- #56: Breaking change: Change return type of `ServiceMethod#returnType()`.
- #55: Add check for circular Service inheritance
- #54: Replace TreeSet with HashMap in service method validation
- #53: Fix: Apply naming policy to method parameter names
- #52: Fix: Crash when parsing certain trailing documentation comments
- #50: Add link-time validation of services and methods
- #48: Fix: keep annotations on type references
- #47: Use `.equals()` instead of reference equality for `ThriftType` comparision
- #43: Add source-type annotations to `Typedef`
- #42: Add `@Deprecated` annotation to generated classes as appropriate
- #40: Add `Struct` interface to generated structured types (thanks @seanabraham)

0.2.3 (released 8 July 2016)
------------------
- #37: Add Obfuscated and Redacted annotations, along with codegen support for PII obfuscation
- #36: Fix references to constants in default values for fields
- #31: Fix parsing `throws` clauses when `throws` is on a separate line

0.2.2 (released 30 March 2016)
------------------
- #26: Fix generated `toString()` for fields with `@redacted` doc comments

0.2.1 (released 29 March 2016)
------------------
- #25: Improve generated `toString()` methods
- #24: Add SimpleJsonProtocol
- #19: Fix codegen for services which inherit from other services
- #5: Fix compilation with relative includes (e.g. `include '../common.thrift'`)
- #7: Fix lookup of included constants during linking
- #4: Add automatic Parcelable implementation
- #2: Fix nested-generic fields with default values

0.2.0 (released 23 February 2016)
------------------

- Re-design `Transport` to not use Okio, avoid potential threading issues therein

0.1.4 (released 16 February 2016)
---------------------------------

- Fix new bug in generated 'toString'

0.1.3 (released 12 February 2016)
---------------------------------

- Alter generated 'toString' so that it outputs one single line
- Make '@redacted' annotation detection case-insensitive

0.1.2 (released 14 January 2016)
--------------------------------

- Demote AssertionError to ProtocolExeception in ProtocolUtil#skip() for unknown TType values.

0.1.1 (released 6 January 2016)
------------------

- Add CompactProtocol implementation
- Add integration test suite
- Add service-client code generation
- Add service-client runtime implementation
- Add ability to parse annotations in Thrift IDL
- Add `(redacted)` annotation for fields


0.1.0 (internal release)
------------------------

- Thrift IDL parser
- Thrift IDL model
- Java code generator
- Command-line compiler
- Generated structs, adapters, and BinaryProtocol
