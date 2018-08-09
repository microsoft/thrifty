Thrifty
=======

[![Build Status](https://travis-ci.org/Microsoft/thrifty.svg?branch=master)](https://travis-ci.org/Microsoft/thrifty) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Thrifty-green.svg?style=true)](https://android-arsenal.com/details/1/3227)
[![codecov](https://codecov.io/gh/Microsoft/thrifty/branch/master/graph/badge.svg)](https://codecov.io/gh/Microsoft/thrifty)


Thrifty is an implementation of the Apache Thrift software stack for Android, which uses 1/4 of the method count taken by 
the Apache Thrift compiler.

Thrift is a widely-used cross-language service-definition software stack, with a nifty interface definition language
from which to generate types and RPC implementations.  Unfortunately for Android devs, the canonical implementation
generates very verbose and method-heavy Java code, in a manner that is not very Proguard-friendly.

Like Square's Wire project for Protocol Buffers, Thrifty does away with getters and setters (and is-setters and
set-is-setters) in favor of public final fields.  It maintains some core abstractions like Transport and Protocol, but
saves on methods by dispensing with Factories and server implementations and only generating code for the
protocols you actually need.

Thrifty was born in the Outlook for Android codebase; before Thrifty, generated thrift classes consumed 20,000 methods.
After Thrifty, the thrift method count dropped to 5,000.

### Usage

In `build.gradle`:

```groovy
repositories {
  mavenCentral()

  // For snapshot builds
  maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
  compile 'com.microsoft.thrifty:thrifty-runtime:0.4.3'
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

### Contributing

We welcome contributions at all levels.  Contributions could be as simple as bug reports and feature suggestions,
typo fixes, additional tests, bugfixes, even new features.  If you wish to contribute code, please be sure to read our
[Contributing Guide](CONTRIBUTING.md).

### Differences with Apache Thrift

Thrifty structs and clients are 100% compatible with Apache Thrift services.

The major differences are:

- Thrifty structs are immutable.
- Thrifty structs are always valid, once built via a builder.
- Fields that are neither required nor optional (i.e. "default") are treated as optional; a struct with an unset default field may still be serialized.
- TupleProtocol and JsonProtocols are unsupported at present.
- Server-specific features from Apache's implementation are not duplicated in Thrifty.

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
java -jar thrifty-compiler.jar --out=path/to/output example.thrift
```

The example file will result in the following files being generated:

```
path/to/output/
  - com/foo/bar/
    - Google.java
    - GoogleClient.java
    - Query.java
    - SearchResult.java
```

The interesting files here are, of course, our domain objects `Query` and `SearchResult`.

The latter looks like this:

```java
package com.foo.bar;

import android.support.annotation.NonNull;
import com.microsoft.thrifty.Adapter;
import com.microsoft.thrifty.Struct;
import com.microsoft.thrifty.StructBuilder;
import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.ThriftField;
import com.microsoft.thrifty.protocol.FieldMetadata;
import com.microsoft.thrifty.protocol.ListMetadata;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.util.ProtocolUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SearchResult implements Struct {
  public static final Adapter<SearchResult, Builder> ADAPTER = new SearchResultAdapter();

  @ThriftField(
      fieldId = 1,
      isRequired = true
  )
  @NonNull
  public final String url;

  /**
   * A list of keywords related to the result
   */
  @ThriftField(
      fieldId = 2,
      isRequired = true
  )
  @NonNull
  public final List<String> keywords;

  /**
   * The time at which the result was last checked, in unix millis
   */
  @ThriftField(
      fieldId = 3,
      isRequired = true
  )
  @NonNull
  public final Long lastUpdatedMillis;

  private SearchResult(Builder builder) {
    this.url = builder.url;
    this.keywords = Collections.unmodifiableList(builder.keywords);
    this.lastUpdatedMillis = builder.lastUpdatedMillis;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof SearchResult)) return false;
    SearchResult that = (SearchResult) other;
    return (this.url == that.url || this.url.equals(that.url))
        && (this.keywords == that.keywords || this.keywords.equals(that.keywords))
        && (this.lastUpdatedMillis == that.lastUpdatedMillis || this.lastUpdatedMillis.equals(that.lastUpdatedMillis));
  }

  @Override
  public int hashCode() {
    int code = 16777619;
    code ^= this.url.hashCode();
    code *= 0x811c9dc5;
    code ^= this.keywords.hashCode();
    code *= 0x811c9dc5;
    code ^= this.lastUpdatedMillis.hashCode();
    code *= 0x811c9dc5;
    return code;
  }

  @Override
  public String toString() {
    return "SearchResult{url=" + this.url + ", keywords=" + this.keywords + ", lastUpdatedMillis=" + this.lastUpdatedMillis + "}";
  }

  @Override
  public void write(Protocol protocol) {
    ADAPTER.write(protocol, this);
  }

  public static final class Builder implements StructBuilder<SearchResult> {
    private String url;

    /**
     * A list of keywords related to the result
     */
    private List<String> keywords;

    /**
     * The time at which the result was last checked, in unix millis
     */
    private Long lastUpdatedMillis;

    public Builder() {
      this.keywords = new ArrayList<String>();
    }

    public Builder(SearchResult struct) {
      this.url = struct.url;
      this.keywords = struct.keywords;
      this.lastUpdatedMillis = struct.lastUpdatedMillis;
    }

    public Builder url(String url) {
      if (url == null) {
        throw new NullPointerException("Required field 'url' cannot be null");
      }
      this.url = url;
      return this;
    }

    public Builder keywords(List<String> keywords) {
      if (keywords == null) {
        throw new NullPointerException("Required field 'keywords' cannot be null");
      }
      this.keywords = keywords;
      return this;
    }

    public Builder lastUpdatedMillis(Long lastUpdatedMillis) {
      if (lastUpdatedMillis == null) {
        throw new NullPointerException("Required field 'lastUpdatedMillis' cannot be null");
      }
      this.lastUpdatedMillis = lastUpdatedMillis;
      return this;
    }

    @Override
    public SearchResult build() {
      if (this.url == null) {
        throw new IllegalStateException("Required field 'url' is missing");
      }
      if (this.keywords == null) {
        throw new IllegalStateException("Required field 'keywords' is missing");
      }
      if (this.lastUpdatedMillis == null) {
        throw new IllegalStateException("Required field 'lastUpdatedMillis' is missing");
      }
      return new SearchResult(this);
    }

    @Override
    public void reset() {
      this.url = null;
      this.keywords = new ArrayList<String>();
      this.lastUpdatedMillis = null;
    }
  }

  private static final class SearchResultAdapter implements Adapter<SearchResult, Builder> {
    // Uninteresting but important serialization code
  }
```

The struct itself is immutable and has a minimal number of methods.  It can be constructed only
with the assistance of a nested `Builder`, which validates that all required fields are set.  Finally, an
Adapter implementation (whose body is omitted here because it is long and mechanical) that handles reading and
writing `SearchResult` structs to and from `Protocols`.

Finally and separately, note `Google` and `GoogleClient` - the former is an interface, and the latter is an autogenerated implementation.

You may notice the similarity to protobuf classes generated by Wire - this is intentional!
The design principles codified there - immutable data, build-time validation, preferring fields over methods,
separating data representation from serialization logic - lead to better, safer code, and more breathing room
for Android applications.

#### Using Generated Code

Given the example above, the code to invoke `Google.search()` might be:

```java

// Transports define how bytes move to and from their destination
SocketTransport transport = new SocketTransport("thrift.google.com", 80);
transport.connect();

// Protocols define the mapping between structs and bytes
Protocol protocol = new BinaryProtocol(transport);

// Generated clients do the plumbing
Google client = new GoogleClient(protocol);

Query query = new Query.Builder()
    .text("thrift vs protocol buffers")
    .build();

// RPC clients are asynchronous and callback-based
client.search(query, new ServiceMethodCallback<List<SearchResult>>() {
    @Override
    public void onSuccess(List<SearchResult> response) {
        // yay
    }

    @Override
    public void onError(Throwable error) {
        Log.e("GoogleClient", "Search error: " + error);
    }
});

```

### Extensibility

Every project has its own requirements, and no one style of boilerplate can fill them all.  Thrifty offers a small but 
powerful plugin model that you can implement, using the standard Java SPI mechanism, which will allow one to customize
each generated Java class before it is written out to disk.  Read more about it in the [thrifty-compiler-plugins README](thrifty-compiler-plugins/README.md).  You can see a worked example in [thrifty-example-postprocessor](thrifty-example-postprocessor).

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

```java
public final class User implements Struct {
  @ThriftField(
    fieldId = 1,
    required = true)
  @Obfuscated
  public final String email;

  @ThriftField(
    fieldId = 2,
    required = true)
  @Redacted
  public final String ssn;

  // more code

  @Override
  public String toString() {
    return "User{email=" + ObfuscationUtil.hash(this.email) + ", ssn=<REDACTED>}";
  }

  // more code
}
```

Obfuscated fields that are collections are not hashed; instead, their type is printed, along with the collection size, e.g. `map<i32, string>(size=5)`.

Close readers will note that the compiler will also respond to `@redacted` and `@obfuscated` in field documentation; this is currently valid *but not supported
and subject to change in future releases*.  It is a legacy from the time before Thrifty implemented Thrift annotations.

### Thanks

Thrifty owes an enormous debt to Square and the Wire team; without them, this project would not exist.  Thanks!
An equal debt is owed to Facebook and Apache for developing and opening Thrift to the world.

-------

Copyright © Microsoft Corporation
