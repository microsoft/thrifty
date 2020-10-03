thrifty-compiler
----------------

A command-line executable that bundles the parser and code-generator together.

Basic usage:

```
java -jar thrifty-compiler.jar \
        --out=path/to/output \
        --path=thrift/search/path \
        --path=other/search/path \
        a.thrift \
        b.thrift
```

Option | Description
------ | ------------
`--out=[dir]` | Required.  Specifies where generated Java files go
`--path=[dir]` | Optional.  Adds a directory to a search path, used for locating included thrift files.
`--lang=[java,kotlin]` | Optional, defaults to java.  Specifies whether to generate Java or Kotlin code.
`--name-style=[default,java]` | Optional.  Specifies how Thrift field names should be represented in generated code.  `default` leaves names unchanged, while `java` converts them to `camelCase`.
`--use-android-annotations` | Optional, deprecated.  Equivalent to `--nullability-annotation-type=android-support`.
`--nullability-annotation-type=[none,android-support,androidx]` | Optional, defaults to `none`.  Specifies whether or not Android nullability annotations will be added for generated fields (`@NonNull` and `@Nullable`) and, if so, which types - those in `android.support.annotation` or `androidx.annotation`.
`--parcelable` | Optional.  Structs, unions, and exceptions will have implementations of Parcelable generated.
`--omit-file-comments` | Optional.  When set, don't add file comments to generated files.
`--list-type=[classname]` | A java.util.List implementation to be used wherever lists are instantiated in generated code.
`--set-type=[classname]` | A java.util.Set implementation to be used wherever sets are instantiated in generated code.
`--map-type=[classname]` | A java.util.Map implementation, as above.
`--kt-file-per-type` | Specifies that one .kt file should be generated per Kotlin type.  The default is for all code to be written to a single file.
