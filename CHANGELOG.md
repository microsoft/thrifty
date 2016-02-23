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
