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
