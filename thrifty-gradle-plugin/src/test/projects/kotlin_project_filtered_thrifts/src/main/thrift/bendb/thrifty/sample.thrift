namespace jvm com.bendb.thrifty.test;

struct Foo {
    1: optional string name;
    2: optional i64 number;
}

service FooFetcher {
    Foo fetchFoo();
}
