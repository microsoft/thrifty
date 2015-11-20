package com.bendb.thrifty;

public enum NamespaceScope {
    ALL("*"),
    CPP("cpp"),
    JAVA("java"),
    PY("py"),
    PERL("perl"),
    RB("rb"),
    COCOA("cocoa"),
    CSHARP("csharp"),
    PHP("php"),
    SMALLTALK_CATEGORY("smalltalk.category"),
    SMALLTALK_PREFIX("smalltalk.prefix");

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
