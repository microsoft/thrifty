Thrifty
=======

"It's like Thrift, but cheaper"

[![Build Status](https://travis-ci.org/benjamin-bader/thrifty.svg?branch=master)](https://travis-ci.org/benjamin-bader/thrifty)

Thrift is super cool, with a nifty interface definition language from which to generate types and RPC implementations.
Unfortunately for Android devs, the canonical implementation generates very verbose and method-heavy Java code.

Like Wire for Protocol Buffers, Thrifty does away with getters and setters in favor of public final fields.  It
maintains the core abstractions like Transport and Protocol, but saves on methods by only generating TupleProtocol-compatible
adapters if configured to do so (not yet implemented).

It it currently completely nonfunctional - in no way, shape, or form is this project usable or useful.  WIP.

### Usage

In `build.gradle`:

```groovy
buildscript {
  repositories {
    mavenCentral()

    // or, for snapshot builds:
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
  }

  dependencies {
    classpath 'com.bendb.thrifty:thrifty-gradle-plugin:0.1.0-SNAPSHOT'
  }
}

apply plugin: 'com.bendb.thrifty'

thrifty {
  searchPath '../inc/thrift'
  thrift '../inc/thrift/MyService.thrift'
}

repositories {
  mavenCentral()
}

dependencies {
  compile 'com.bendb.thrifty:thrifty-runtime:0.1.0-SNAPSHOT'
}
```

### Building

```bash
./gradlew build
```

### Testing

```bash
./gradlew check
```

### Deploying

```bash
./gradlew uploadArchives
```

You will need to have valid Sonatype Nexus OSS credentials, as well as a valid *and published* GPG signing key, configured in your local `gradle.properties` file.

### Credit

Inspired by and borrowing from Square's Wire project, particularly the parser architecture
Certain utilities borrowed (TType, BinaryProtcol) from the official Apache Thrift implementation

-------

Copyright © 2015 Benjamin Bader