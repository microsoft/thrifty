Thrifty
=======

"It's Thrift, but cheaper"

[![Build Status](https://travis-ci.org/benjamin-bader/thrifty.svg?branch=master)](https://travis-ci.org/benjamin-bader/thrifty)

Thrifty is an implementation of the Apache Thrift software stack for Android.

Thrift is a widely-used cross-language service-defition software stack, with a nifty interface definition language
from which to generate types and RPC implementations.  Unfortunately for Android devs, the canonical implementation
generates very verbose and method-heavy Java code, in a manner that is not very Proguard-friendly.

Like Square's Wire project for Protocol Buffers, Thrifty does away with getters and setters (and is-set and
set-is-setters) in favor of public final fields.  It maintains some core abstractions like Transport and Protocol, but
saves on methods by dispensing with Factories and only only generating code for the protocols you actually need.

Code generation for everything but services is complete.  If you are not using services, Thrifty is usable today for
serializing and deserializing Thrifts.  Services are coming as soon as runtime support is ironed out.

### Usage

In `build.gradle`:

```groovy
repositories {
  mavenCentral()

  // For snapshot builds
  maven { url https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
  compile 'com.bendb.thrifty:thrifty-runtime:0.1.0-SNAPSHOT'
}
```

On the command line:

```bash
java -jar thrifty-compiler.jar --out=path/to/output file_one.thrift file_two.thrift file_n.thrift
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

-------

Copyright © 2015 Benjamin Bader