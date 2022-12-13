3.1.0 (released 13 December 2022)
------------------
- Add: Support default field values in struct-typed constants (#507)
- Add: Implement Java codegen for struct-typed constants (#503)
- Add: Sort constants in Schema by "dependency order" (#502)
- Add: Added `Constant#referencedConstants` list (#499)
- Add: new Gradle plugin support for TypeProcessors (#494)
- Change: Bump Okio to 3.2.0 (#510)
- Change: Bump Kotlin to 1.7.20 (#495)
- Fix: avoid afterEvaluate in thrifty-gradle-plugin (#492)

3.1.0-RC02 (released 7 June 2022)
------------------
- Add preliminary support for TypeProcessor plugins to thrifty-gradle-plugin (#490)
- Add "generated" comments to Kotlin files (thanks @shashachu) (#486)
- Add Kotlin codegen for struct-value constants (#482)
- Maybe break?  Removed deprecated `@JvmDefault` annotations from thrifty-runtime (#481)

3.1.0-RC01 (released 13 April 2022)
------------------
- Add struct-valued constant validator to thirfty-schema (thanks @janarajan) (#467)
- Add server support (thanks @luqasn) (#447)
- Bump Kotlin to 1.6.20

3.0.0 (released 7 August 2021)
------------------
The major update in this release is the transition of Thrifty from a Java project to a Kotlin Multiplatform project.  Among other changes, one thing to note is that the runtime artifact `thrifty-runtime` now refers to an MPP artifact.  The correct artifact to reference in your existing Android or Java projects is `thrifty-runtime-jvm`.

- BREAK: All support for `@Generated` annotations has been removed (#402)
- BREAK: thrifty-runtime ported to Kotlin Multiplatform (#401)
- BREAK: thrifty-runtime-ktx is gone, and has been merged into thrifty-runtime (#397)
- BREAK: thrifty-runtime ported to Kotlin (#391)
- BREAK: Minimum supported JDK is now version 8 (#391)
- BREAK: Fields whose names are "soft" or "modifier" Kotlin keywords now have an underscore suffix (thanks @luqasn) (#446)
- Add Okio-based convenience APIs to thrifty-runtime (#408)
- Add big-enum mode to enable enums with large numbers of members (Thanks @shashachu) (#421)
- Change: Kotlin structs are builderless by default (#414)
- Change: Gradle plugin defaults to Kotlin (#442)
- Change: thrifty-compiler defaults to generating Kotlin (#451)
- Fix: Empty structs use literal class name for hashCode (#415)
- Fix: `Location` in thrifty-schema should always be an include root (#416)
- Fix: Make `@JvmStatic` annotations opt-in (#417)
- Fix: Including sibling .thrift files now works (#434)
- Fix: Unions with fields named `error` (thanks @luqasn) (#444)


3.0.0-RC-2 (released 2 August 2021)
------------------
- Fix broken Maven publication

3.0.0-RC01 (released 21 June 2021)
------------------
- BREAK: All support for `@Generated` annotations has been removed (#402)
- BREAK: thrifty-runtime-ktx is gone, and has been merged into thrifty-runtime (#397)
- BREAK: thrifty-runtime ported to Kotlin (#391)
- BREAK: Minimum supported JDK is now version 8 (#391)
- BREAK: Fields whose names are "soft" or "modifier" Kotlin keywords now have an underscore suffix (thanks @luqasn) (#446)
- Add Okio-based convenience APIs to thrifty-runtime (#408)
- Add big-enum mode to enable enums with large numbers of members (Thanks @shashachu) (#421)
- Change: Kotlin structs are builderless by default (#414)
- Change: Gradle plugin defaults to Kotlin (#442)
- Change: thrifty-compiler defaults to generating Kotlin (#451)
- Fix: Empty structs use literal class name for hashCode (#415)
- Fix: `Location` in thrifty-schema should always be an include root (#416)
- Fix: Make `@JvmStatic` annotations opt-in (#417)
- Fix: Including sibling .thrift files now works (#434)
- Fix: Unions with fields named `error` (thanks @luqasn) (#444)

2.1.1 (released 13 July 2020)
------------------
- #369: Gradle plugin: Rename includeDir to includePath, customize outputDirectory
- #368: Fix infinite loop in FramedTransport#read on EOF (thanks @denisov-mikhail)
- #367: Gradle plugin: Add API to configure which specific thrift files are compiled

2.1.0 (released 9 June 2020)
------------------
- #362: Fix: Explicitly fail service calls on IOException or RuntimeException (thanks @amorozov)
- #357: Add --no-fail-on-default-enum-values (thanks @guptadeepanshu)
- #356: Add thrifty-gradle-plugin to compile thrifts in Gradle JVM projects
- #348: Change: Update okio from 1.14.1 to 2.6.0
- #332: Add --kt-emit-jvmname option (thanks @jparise)
- #330: Add --omit-service-clients option (thanks @jparise)

2.0.1 (released 4 May 2020)
------------------
Rebuild with JDK8, not JDK11

2.0.0 (released 28 April 2020, Quarantine Edition)
------------------
- #328: Fix: Print LoadFailedException cause on load failure (thanks @jparise)
- #326: Fix: Fix Java compilation with catch-all thrift namespaces (thanks @timvlaer)
- #317: Fix: Use UTF-8 for strings in SimpleJsonProtocol

2.0.0-RC1 (released 8 May 2019)
------------------
- #307: Support AndroidX nullability annotations (thanks, @DSteve595)
- #305: Show location of including file when included file is not found (thanks, @hzsweers)
- #300: Fail parsing when the parser terminates without reading the entire input
- #253: Use sealed types for unions in generated Kotlin code (thanks, @Zomzog)
- General: Represent Thrift unions with sealed types in Kotlin
- #273: Fix circular-include error messages

1.0.0 (released 2 November 2018)
------------------
- #259: Enable configurable `@Generated` annotation type
- #254: Update to Kotlin 1.3, update generated coroutine APIs to be non-experimental
- #250: Add `@NonNull` to struct builder's copy ctor (thanks, @jparise)
- #241: Add nullable annotation to gen'd `findByValue` methods (thanks, @jparise)
- #239: Fix: Canonicalize paths of all loaded thrift files
- #236: Only use base filename in constants' comments (thanks, @jparise)

1.0.0-RC2 (released 10 September 2018)
------------------
- #235: Emit `@Generated` annotations on generated types
- #234: Add '--omit-file-comments' compiler flag (thanks, @jparise)
- #232: Add synthetic "JVM" namespace scope (yet again, thanks @hzsweers)
- #231: Add "functional-equality" implementation for schemas (thanks, @hzsweers)
- #230: Add rendering tool for thrifty-schema (thanks, @hzsweers)

1.0.0-RC1 (released 15 August 2018)
------------------
- #225: Add Kotlin coroutine-based service client APIs
- #224: Fix ClientBase by adding missing call to protocol.readMessageEnd()
- #223: Fix FramedTransport reads over > 1 frame
- #217: Avoid scope collisions when reading map values (thanks again, @jparise!)
- #216: Fix const validation for doubles (thanks, @jparise)
- General: Broke thrifty-schema API in favor of idiomatic Kotlin
- #204: Remove deprecated java.io.File methods in Loader
- #183: Added Kotlin codegen
- #178: Fix: .thrift files in include paths during path scanning
- #165: Adopted Guava's case formatter for FieldNamingPolicy
- #164: Fixed nullability annotations for struct builders
- #161: Generate @Nullable annotations for union fields

0.4.3 (released 8 January 2018)
------------------
- #156: Add JSON protocol support
- #153: Fix reading bool value in CompactProtocol
- #152: Add UUID for distinguishing parsed elements from those modified via `newBuilder`
- #147: Enable synchronous service calls

0.4.2 (released 2 May 2017)
------------------
- #141: Add missing method EnumMember#toBuilder()
- #139: Fix crash when thrift doc comments had `$L` or other JavaPoet sigils

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
