package com.bendb.thrifty;

import com.squareup.javapoet.ClassName;
import okio.ByteString;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ThriftyCodeGenerator {
    static final ClassName STRING = ClassName.get(String.class);
    static final ClassName LIST = ClassName.get(List.class);
    static final ClassName MAP = ClassName.get(Map.class);
    static final ClassName SET = ClassName.get(Set.class);
    static final ClassName BYTE_STRING = ClassName.get(ByteString.class);

    static final ClassName TADAPTER = ClassName.get(ThriftAdapter.class);
    static final ClassName TEXCEPTION = ClassName.get(TException.class);
    static final ClassName TFIELD = ClassName.get(ThriftField.class);
    static final ClassName TTYPE = ClassName.get(TType.class);

    private final ThriftProtocol adapterProtocol;

    public ThriftyCodeGenerator(ThriftProtocol adapterProtocol) {
        this.adapterProtocol = adapterProtocol;
    }
}
