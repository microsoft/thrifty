Gradle Plugin
-------------------

**Under active development**

Thrifty's Gradle plugin allows one to incorporate .thrift source files into a Java/Kotlin project
and have Thrifty generate Java/Kotlin code from them as part of regular builds.

THIS DOES NOT YET WORK AT ALL, DO NOT EVEN TRY TO USE IT.

When ready, incorporate it into your build like so:

```gradle
apply plugin: 'kotlin'
apply plugin: 'com.microsoft.thrifty'

// usual gradle stuff

// Configuration is optional as there are generally sane defaults
thrifty {
    // By default, thrifty will use all thrift files in 'src/main/thrift'.
    // You can override this in the following ways:

    // Specify one or more individual files.  Note that if you do, 'src/main/thrift' is ignored.
    thriftFile 'path/to/file.thrift', 'path/to/otherFile.thrift'

    // Augment the thrift include path with one or more directories:
    includeDir 'path/to/include/dir', 'path/to/other/dir'
}
```
