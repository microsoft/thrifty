Thrifty
=======

![Build status](https://github.com/microsoft/thrifty/workflows/Pre-merge%20checks/badge.svg) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Thrifty-green.svg?style=true)](https://android-arsenal.com/details/1/3227)
[![codecov](https://codecov.io/gh/Microsoft/thrifty/branch/master/graph/badge.svg)](https://codecov.io/gh/Microsoft/thrifty)


Thrifty is an implementation of the Apache Thrift software stack, which uses 1/4 of the method count taken by
the Apache Thrift compiler, which makes it especially appealing for use on Android.

Thrift is a widely-used cross-language service-definition software stack, with a nifty interface definition language
from which to generate types and RPC implementations.  Unfortunately for Android devs, the canonical implementation
generates very verbose and method-heavy Java code, in a manner that is not very Proguard-friendly.

Like Square's Wire project for Protocol Buffers, Thrifty does away with getters and setters (and is-setters and
set-is-setters) in favor of public final fields.  It maintains some core abstractions like Transport and Protocol, but
saves on methods by dispensing with Factories, omitting server implementations by default and only generating code for the
protocols you actually need.

Thrifty was born in the Outlook for Android codebase; before Thrifty, generated thrift classes consumed 20,000 methods.
After Thrifty, the thrift method count dropped to 5,000.

### Usage

#### Add the runtime to your project

In `build.gradle`:

```groovy
repositories {
  mavenCentral()

  // For snapshot builds
  maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
  implementation 'com.microsoft.thrifty:thrifty-runtime-jvm:3.0.0'
}
```

#### Generate code from your thrift files

On the command line:

```bash
java -jar thrifty-compiler.jar --out=path/to/output file_one.thrift file_two.thrift file_n.thrift
```

Or, with the Gradle plugin:

```groovy

buildscript {
  dependencies {
    classpath 'com.microsoft.thrifty:thrifty-gradle-plugin:3.0.0'
  }
}

apply plugin: 'com.microsoft.thrifty'

thrifty {
  // Optionally configure things, see thrifty-gradle-plugin/README.md
  // for all the details
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

### Contributing

We welcome contributions at all levels.  Contributions could be as simple as bug reports and feature suggestions,
typo fixes, additional tests, bugfixes, even new features.  If you wish to contribute code, please be sure to read our
[Contributing Guide](CONTRIBUTING.md).

### Differences with Apache Thrift

Thrifty structs and clients are 100% compatible with Apache Thrift services.

The major differences are:

- Thrifty structs are immutable.
- Thrifty structs are always valid, once built.
- Fields that are neither required nor optional (i.e., "default") are treated as optional; a struct with an unset default field may still be serialized.
- TupleProtocol is unsupported at present.
- Server-specific features are only supported for Kotlin code generation and currently considered experimental

## Guide To Thrifty

Thrift is a language-agnostic remote-procedure-call (RPC) definition toolkit.  Services, along with a rich set of
structured data, are defined using the Thrift Interface Definition Language (IDL).  This IDL is then compiled into
one or more target languages (e.g. Java), where it can be used as-is to invoke RPC methods on remote services.

Thrifty is an alternate implementation of Thrift targeted at Android usage.  Its benefits over the standard Apache
implementation are its greatly reduced method count and its increased type-safety.  By generating immutable classes
that are validated before construction, consuming code doesn't have to constantly check if required data is set or not.

#### Interface Definition Language

The Thrift IDL is a simple and standardized way to define data, data structures, and services:

```thrift
// Let's call this example.thrift

namespace java com.foo.bar

struct Query {
  1: required string text,
  2: optional i64 resultsNewerThan
}

struct SearchResult {
  1: required string url,
  2: required list<string> keywords = [], // A list of keywords related to the result
  3: required i64 lastUpdatedMillis // The time at which the result was last checked, in unix millis
}

service Google {
  list<SearchResult> search(1: Query query)
}
```

For an authoritative source on Thrift IDL, [Thrift: The Missing Guide](https://diwakergupta.github.io/thrift-missing-guide/) is an excellent introduction.

#### Generating Code

Use `thrifty-compiler` to compile IDL into Java classes:

```bash
java -jar thrifty-compiler.jar --kt-file-per-type --out=path/to/output example.thrift
```

The example file will result in the following files being generated:

```
path/to/output/
  - com/foo/bar/
    - Google.kt
    - GoogleClient.kt
    - Query.kt
    - SearchResult.kt
```

The interesting files here are, of course, our domain objects `Query` and `SearchResult`.

The latter looks like this:

```kotlin
package com.foo.bar

import com.microsoft.thrifty.Struct
import com.microsoft.thrifty.TType
import com.microsoft.thrifty.ThriftField
import com.microsoft.thrifty.kotlin.Adapter
import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.util.ProtocolUtil
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.jvm.JvmField

public data class SearchResult(
  @JvmField
  @ThriftField(fieldId = 1, isRequired = true)
  public val url: String,

  @JvmField
  @ThriftField(fieldId = 2, isRequired = true)
  public val keywords: List<String>,

  @JvmField
  @ThriftField(fieldId = 3, isRequired = true)
  public val lastUpdatedMillis: Long
) : Struct {
  public override fun write(protocol: Protocol): Unit {
    ADAPTER.write(protocol, this)
  }

  private class SearchResultAdapter : Adapter<SearchResult> {
    // Uninteresting but important serialization code
  }

  public companion object {
    @JvmField
    public val ADAPTER: Adapter<SearchResult> = SearchResultAdapter()
  }
}
```

The struct itself is immutable and has a minimal number of methods.  It can be constructed only
with all required fields (all of them, in this example).  An Adapter implementation (whose body we
omit here because it is long and mechanical) handles reading and writing `SearchResult` structs to
and from `Protocols`.

Finally and separately, note `Google` and `GoogleClient` - the former is an interface, and the latter is an autogenerated implementation.

You may notice the similarity to protobuf classes generated by Wire - this is intentional!
The design principles codified there - immutable data, build-time validation, preferring fields over methods,
separating data representation from serialization logic - lead to better, safer code, and more breathing room
for Android applications.

#### Using Generated Code

Given the example above, the code to invoke `Google.search()` might be:

```kotlin
// Transports define how bytes move to and from their destination
val transport = SocketTransport.Builder("thrift.google.com", 80).build().apply { connect() }

// Protocols define the mapping between structs and bytes
val protocol = BinaryProtocol(transport)

// Generated clients do the plumbing
val client = GoogleClient(protocol, object : AsyncClientBase.Listener {
    override fun onTransportClosed() {

    }

    override fun onError(throwable: Throwable) {
        throw AssertionError(throwable)
    }
})

val query = Query(text = "thrift vs protocol buffers")

// RPC clients are asynchronous and callback-based
client.search(query, object : ServiceMethodCallback<List<SearchResult>> {
    override fun onSuccess(response: List<SearchResult>) {
        // yay
    }

    override fun onError(throwable: Throwable) {
        Log.e("GoogleClient", "Search error: $throwable")
    }
})

// ...unless coroutine clients were generated:
val results = async { client.search(query) }.await()

```

### Extensibility

Every project has its own requirements, and no one style of boilerplate can fill them all.  Thrifty offers a small but 
powerful plugin model that you can implement, using the standard Java SPI mechanism, which will allow one to customize
each generated class before it is written out to disk.  Read more about it in the [thrifty-compiler-plugins README](thrifty-compiler-plugins/README.md).  You can see a worked example in [thrifty-example-postprocessor](thrifty-example-postprocessor).

### Hiding PII with Redaction and Obfuscation

Personally-Identifiable Information (PII) is an inevitability in most systems, and often there are legal consequences
if it is not handled carefully.  Thrifty allows you to avoid logging PII contained in generated classes by supporting
both total redaction and obfuscation.  It is as simple as adding annotations to your Thrift IDL:

```thrift
struct User {
  1: required string email (obfuscated)
  2: required string ssn (redacted)
}
```

The difference between redaction and obfuscation is small but important.  In `.toString()`, `redacted` fields are totally replaced
with the string `"<REDACTED>"` - no information survives.  This meets the goal of not leaking PII, but has the consequence that
sometimes debugging can be difficult.  `obfuscated` fields, on the other hand, are treated differently.  Their values are hashed,
and this hash is printed.  This allows one to distinguish between unique values in log files, without compromising user privacy.

The Thrift annotations `(thrifty.redacted)` and `(thrifty.obfuscated)` are also accepted by the compiler.

The Thrift example above leads to code similar to the following:

```kotlin
public data class User(
    @JvmField
    @ThriftField(fieldId = 1, required = true)
    @Obfuscated
    public val email: String,

    @JvmField
    @ThriftField(fieldId = 2, required = true)
    @Redacted
    public val ssn: String
) : Struct {
  public override fun toString() =
      "User(email=${ObfuscationUtil.hash(email)}, ssn=<REDACTED>)"

  // more code
}
```

Obfuscated fields that are collections are not hashed; instead, their type is printed, along with the collection size, e.g. `map<i32, string>(size=5)`.

Close readers will note that the compiler will also respond to `@redacted` and `@obfuscated` in field documentation; this is currently valid *but not supported
and subject to change in future releases*.  It is a legacy from the time before Thrifty implemented Thrift annotations.

## Java Support

Thrifty generates Kotlin code by default, but if needed it can also produce Java.  Generated Java code has very slightly
more method references than Kotlin.  Instead of data classes, Java structs are final classes with public final fields,
and are constructable only with dedicated `Builder` types.

To generate Java classes, pass `--lang=java` to the compiler (if using the command-line compiler), or provide a `java {}` block
within the thrifty Gradle plugin configuration block.

## Language-specific command-line options
### Kotlin-specific command-line options

There are a few new command-line options to control Kotlin code generation:

```
java -jar thrifty-compiler.jar \
    --lang=kotlin \
    --service-type=coroutine \
    --kt-file-per-type \
    --omit-file-comments \
    --kt-struct-builders \
    --experimental-kt-generate-server \
    ...
```

By default, generated service clients are callback-based:

```kotlin
public interface Google {
  public fun search(query: Query, callback: ServiceMethodCallback<List<SearchResult>>)
}
```

If, instead, you wish to have a coroutine-based client, specify `--service-type=coroutine`:

```kotlin
public interface Google {
  public suspend fun search(query: Query): List<SearchResult>
}
```

Builders are unnecessary, and are not included by default.  For compatibility with older code, you can use the `--kt-struct-builders` flag, which will result in Java-style classes with Builders.

By default, Thrifty generates one Kotlin file per JVM package.  For larger thrift files, this can be a little hard on the Kotlin compiler.  If you find build times or IDE performance suffering, the `--kt-file-per-type` flag can help.  Outlook Mobile's single, large, Kotlin file took up to one minute just to typecheck, using Kotlin 1.2.51!  For these cases, `--kt-file-per-type` will tell Thrifty to generate one single file per top-level class - just like the Java code.

`--experimental-kt-generate-server` enabled code generation for the server portion of a thrift service. You can
use this to implement a thrift server with the same benefits as the kotlin client: no runtime surprises thanks to
structs being always valid by having nullability guarantees and unions represented as sealed classes.
See [Server Support](#server-support).

#### Server Support
Support for generating a Kotlin server implementation was only added very recently, and while it passes the 'official'
[Java client integration test](https://github.com/apache/thrift/blob/master/lib/java/test/org/apache/thrift/test/TestClient.java),
you should consider this code experimental.

Thrifty generates a `Processor` implementation that you pass an input `Protocol`, an output `Protocol` and a service handler
and the code will take care of reading the request, passing it to the handler and returning the correct response to the output.

If you want to use it, you need to wrap an appropriate communication layer around it, e.g. an HTTP server.
You can have a look at the [integration tests](thrifty-integration-tests/src/test/kotlin/com/microsoft/thrifty/integration/conformance/server/TestServer.kt) for a basic example.

### Java-specific command-line options

Thrifty can be made to add various kinds of nullability annotations to Java types with the `--nullability-annotation-type` flag.  Valid options are
`none` (the default), `android-support`, and `androidx`.  Specifying `android-support` will cause generated code to use `@Nullable` and `@NonNull` from
the `android.support.annotation` package.  Similarly, specifying `androidx` will use analogous annotations from `androidx.annotation`. 

## Thanks

Thrifty owes an enormous debt to Square and the Wire team; without them, this project would not exist.  Thanks!
An equal debt is owed to Facebook and Apache for developing and opening Thrift to the world.

-------

Copyright © Microsoft Corporation
