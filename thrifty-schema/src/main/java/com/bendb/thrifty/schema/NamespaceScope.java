package com.bendb.thrifty.schema;

public enum NamespaceScope {
    ALL("*"),
    CPP("cpp"),
    JAVA("java"),
    PY("py"),
    PY_TWISTED("py.twisted"),
    PERL("perl"),
    RB("rb"),
    COCOA("cocoa"),
    CSHARP("csharp"),
    PHP("php"),
    SMALLTALK_CATEGORY("smalltalk.category"),
    SMALLTALK_PREFIX("smalltalk.prefix"),
    C_GLIB("cglib"),
    GO("go"),
    LUA("lua"),
    ST("st"),
    DELPHI("delphi"),
    JAVASCRIPT("js"),
    UNKNOWN("none");

    private final String name;

    NamespaceScope(String name) {
        this.name = name;
    }

    public static NamespaceScope forThriftName(String name) {
        for (NamespaceScope scope : values()) {
            if (scope.name.equals(name)) {
                return scope;
            }
        }
        return null;
    }
}
