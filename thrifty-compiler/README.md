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

Options:

`--out=[dir]`: Required.  Specifies where generated Java files go
`--path=[dir]`: Optional.  Adds a directory to a search path, used for locating included thrift files.
`--use-android-annotations`: Optional.  When given, Android Support Annotations will be added for generated fields (`@NonNull` and `@Nullable`).
`--list-type=[classname]`: A java.util.List implementation to be used wherever lists are instantiated in generated code.
`--set-type=[classname]`: A java.util.Set implementation to be used wherever sets are instantiated in generated code.
`--map-type=[classname]`: A java.util.Map implementation, as above.

