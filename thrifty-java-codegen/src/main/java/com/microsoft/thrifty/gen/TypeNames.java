/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.gen;

import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.Adapter;
import com.microsoft.thrifty.StructBuilder;
import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.ThriftException;
import com.microsoft.thrifty.protocol.FieldMetadata;
import com.microsoft.thrifty.protocol.ListMetadata;
import com.microsoft.thrifty.protocol.MapMetadata;
import com.microsoft.thrifty.protocol.MessageMetadata;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.protocol.SetMetadata;
import com.microsoft.thrifty.service.AsyncClientBase;
import com.microsoft.thrifty.service.MethodCall;
import com.microsoft.thrifty.service.ServiceMethodCallback;
import com.microsoft.thrifty.service.TMessageType;
import com.microsoft.thrifty.util.ObfuscationUtil;
import com.microsoft.thrifty.util.ProtocolUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import okio.ByteString;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * JavaPoet type names used for code generation.
 */
final class TypeNames {
    static final TypeName BOOLEAN = ClassName.BOOLEAN.box();
    static final TypeName BYTE = ClassName.BYTE.box();
    static final TypeName SHORT = ClassName.SHORT.box();
    static final TypeName INTEGER = ClassName.INT.box();
    static final TypeName LONG = ClassName.LONG.box();
    static final TypeName DOUBLE = ClassName.DOUBLE.box();
    static final TypeName VOID = ClassName.VOID; // Don't box void, it is only used for methods returning nothing.

    static final ClassName COLLECTIONS = ClassName.get(Collections.class);
    static final ClassName STRING = ClassName.get(String.class);
    static final ClassName LIST = ClassName.get(List.class);
    static final ClassName MAP = ClassName.get(Map.class);
    static final ClassName MAP_ENTRY = ClassName.get(Map.Entry.class);
    static final ClassName SET = ClassName.get(Set.class);
    static final ClassName BYTE_STRING = ClassName.get(ByteString.class);
    static final ClassName STRING_BUILDER = ClassName.get(StringBuilder.class);

    static final ClassName LIST_META = ClassName.get(ListMetadata.class);
    static final ClassName SET_META = ClassName.get(SetMetadata.class);
    static final ClassName MAP_META = ClassName.get(MapMetadata.class);

    static final ClassName PROTOCOL = ClassName.get(Protocol.class);
    static final ClassName PROTO_UTIL = ClassName.get(ProtocolUtil.class);
    static final ClassName PROTOCOL_EXCEPTION = ClassName.get(ProtocolException.class);
    static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);
    static final ClassName EXCEPTION = ClassName.get(Exception.class);
    static final ClassName TTYPE = ClassName.get(TType.class);
    static final ClassName TMESSAGE_TYPE = ClassName.get(TMessageType.class);

    static final ClassName THRIFT_EXCEPTION = ClassName.get(ThriftException.class);
    static final ClassName THRIFT_EXCEPTION_KIND = ClassName.get(ThriftException.Kind.class);

    static final ClassName BUILDER = ClassName.get(StructBuilder.class);
    static final ClassName ADAPTER = ClassName.get(Adapter.class);

    static final ClassName FIELD_METADATA = ClassName.get(FieldMetadata.class);
    static final ClassName MESSAGE_METADATA = ClassName.get(MessageMetadata.class);

    static final ClassName NOT_NULL = ClassName.get("android.support.annotation", "NonNull");
    static final ClassName NULLABLE = ClassName.get("android.support.annotation", "Nullable");

    static final ClassName SERVICE_CALLBACK = ClassName.get(ServiceMethodCallback.class);
    static final ClassName SERVICE_CLIENT_BASE = ClassName.get(AsyncClientBase.class);
    static final ClassName SERVICE_CLIENT_LISTENER = ClassName.get(AsyncClientBase.Listener.class);
    static final ClassName SERVICE_METHOD_CALL = ClassName.get(MethodCall.class);

    static final ClassName PARCEL = ClassName.get("android.os", "Parcel");
    static final ClassName PARCELABLE = ClassName.get("android.os", "Parcelable");
    static final ClassName PARCELABLE_CREATOR = ClassName.get("android.os", "Parcelable", "Creator");

    static final ClassName OBFUSCATION_UTIL = ClassName.get(ObfuscationUtil.class);

    /**
     * A mapping of {@link TType} constant values to their Java names.
     */
    private static final ImmutableMap<Byte, String> TTYPE_NAMES;

    static {
        ImmutableMap.Builder<Byte, String> map = ImmutableMap.builder();
        map.put(TType.BOOL, "BOOL");
        map.put(TType.BYTE, "BYTE");
        map.put(TType.I16, "I16");
        map.put(TType.I32, "I32");
        map.put(TType.I64, "I64");
        map.put(TType.DOUBLE, "DOUBLE");
        map.put(TType.STRING, "STRING");
        map.put(TType.ENUM, "ENUM");
        map.put(TType.STRUCT, "STRUCT");
        map.put(TType.LIST, "LIST");
        map.put(TType.SET, "SET");
        map.put(TType.MAP, "MAP");
        map.put(TType.VOID, "VOID");
        map.put(TType.STOP, "STOP");
        TTYPE_NAMES = map.build();
    }

    /**
     * Gets the {@link TType} member name corresponding to the given type-code.
     *
     * @param code the code whose name is needed
     * @return the TType member name as a string
     */
    static String getTypeCodeName(byte code) {
        if (!TTYPE_NAMES.containsKey(code)) {
            throw new NoSuchElementException("not a TType member: " + code);
        }
        return TTYPE_NAMES.get(code);
    }

    private TypeNames() {
        // no instances
    }
}
