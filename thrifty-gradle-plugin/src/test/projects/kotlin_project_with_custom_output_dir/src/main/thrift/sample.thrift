namespace jvm com.bendb.thrifty.test;

include "common.thrift";

struct Foo {
    1: optional string name;
    2: optional i64 number;
}

service FooFetcher {
    common.Foo fetchFoo();
}
