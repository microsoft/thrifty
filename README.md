Thrifty

"It's like Thrift, but cheaper"

[![Build Status](https://travis-ci.org/benjamin-bader/thrifty.svg?branch=master)](https://travis-ci.org/benjamin-bader/thrifty)

Thrift is super cool, with a nifty interface definition language from which to generate types and RPC implementations.
Unfortunately for Android devs, the canonical implementation generates very verbose and method-heavy Java code.

Like Wire for Protocol Buffers, Thrifty does away with getters and setters in favor of public final fields.  It
maintains the core abstractions like Transport and Protocol, but saves on methods by only generating TupleProtocol-compatible
adapters if configured to do so (not yet implemented).

It it currently completely nonfunctional - in no way, shape, or form is this project usable or useful.  WIP.