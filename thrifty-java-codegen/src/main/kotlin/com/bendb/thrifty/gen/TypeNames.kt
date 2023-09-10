/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.gen

import com.bendb.thrifty.Adapter
import com.bendb.thrifty.Obfuscated
import com.bendb.thrifty.Redacted
import com.bendb.thrifty.StructBuilder
import com.bendb.thrifty.TType
import com.bendb.thrifty.ThriftException
import com.bendb.thrifty.ThriftField
import com.bendb.thrifty.protocol.FieldMetadata
import com.bendb.thrifty.protocol.ListMetadata
import com.bendb.thrifty.protocol.MapMetadata
import com.bendb.thrifty.protocol.MessageMetadata
import com.bendb.thrifty.protocol.Protocol
import com.bendb.thrifty.protocol.SetMetadata
import com.bendb.thrifty.service.AsyncClientBase
import com.bendb.thrifty.service.MethodCall
import com.bendb.thrifty.service.ServiceMethodCallback
import com.bendb.thrifty.service.TMessageType
import com.bendb.thrifty.util.ObfuscationUtil
import com.bendb.thrifty.util.ProtocolUtil
import com.squareup.javapoet.ClassName
import okio.ByteString

import java.io.IOException
import java.lang.Deprecated
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.net.ProtocolException
import java.util.Collections
import java.util.NoSuchElementException

/**
 * JavaPoet type names used for code generation.
 */
internal object TypeNames {
    val BOOLEAN = ClassName.BOOLEAN.box()
    val BYTE = ClassName.BYTE.box()
    val SHORT = ClassName.SHORT.box()
    val INTEGER = ClassName.INT.box()
    val LONG = ClassName.LONG.box()
    val DOUBLE = ClassName.DOUBLE.box()
    val VOID = ClassName.VOID // Don't box void, it is only used for methods returning nothing.

    val COLLECTIONS = classNameOf<Collections>()
    val STRING = classNameOf<String>()
    val LIST = classNameOf<List<*>>()
    val MAP = classNameOf<Map<*, *>>()
    val MAP_ENTRY = classNameOf<Map.Entry<*, *>>()
    val SET = classNameOf<Set<*>>()
    val BYTE_STRING = classNameOf<ByteString>()
    val STRING_BUILDER = classNameOf<StringBuilder>()
    val ILLEGAL_STATE_EXCEPTION = classNameOf<java.lang.IllegalStateException>()
    val ILLEGAL_ARGUMENT_EXCEPTION = classNameOf<java.lang.IllegalArgumentException>()
    val NULL_POINTER_EXCEPTION = classNameOf<java.lang.NullPointerException>()

    val ARRAY_LIST = classNameOf<ArrayList<*>>()
    val LINKED_HASH_MAP = classNameOf<LinkedHashMap<*, *>>()
    val LINKED_HASH_SET = classNameOf<LinkedHashSet<*>>()

    val LIST_META = classNameOf<ListMetadata>()
    val SET_META = classNameOf<SetMetadata>()
    val MAP_META = classNameOf<MapMetadata>()

    val PROTOCOL = classNameOf<Protocol>()
    val PROTO_UTIL = classNameOf<ProtocolUtil>()
    val PROTOCOL_EXCEPTION = classNameOf<ProtocolException>()
    val IO_EXCEPTION = classNameOf<IOException>()
    val EXCEPTION = classNameOf<java.lang.Exception>()
    val TTYPE = classNameOf<TType>()
    val TMESSAGE_TYPE = classNameOf<TMessageType>()

    val THRIFT_EXCEPTION = classNameOf<ThriftException>()
    val THRIFT_EXCEPTION_KIND = classNameOf<ThriftException.Kind>()

    val BUILDER = classNameOf<StructBuilder<*>>()
    val ADAPTER = classNameOf<Adapter<*, *>>()

    val FIELD_METADATA = classNameOf<FieldMetadata>()
    val MESSAGE_METADATA = classNameOf<MessageMetadata>()

    val OVERRIDE = classNameOf<Override>()
    val DEPRECATED = classNameOf<java.lang.Deprecated>()
    val SUPPRESS_WARNINGS = classNameOf<java.lang.SuppressWarnings>()
    val REDACTED = classNameOf<Redacted>()
    val OBFUSCATED = classNameOf<Obfuscated>()
    val THRIFT_FIELD = classNameOf<ThriftField>()

    val ANDROID_SUPPORT_NOT_NULL = ClassName.get("android.support.annotation", "NonNull")
    val ANDROID_SUPPORT_NULLABLE = ClassName.get("android.support.annotation", "Nullable")
    val ANDROIDX_NOT_NULL = ClassName.get("androidx.annotation", "NonNull")
    val ANDROIDX_NULLABLE = ClassName.get("androidx.annotation", "Nullable")

    val SERVICE_CALLBACK = classNameOf<ServiceMethodCallback<*>>()
    val SERVICE_CLIENT_BASE = classNameOf<AsyncClientBase>()
    val SERVICE_CLIENT_LISTENER = classNameOf<AsyncClientBase.Listener>()
    val SERVICE_METHOD_CALL = classNameOf<MethodCall<*>>()

    val PARCEL = ClassName.get("android.os", "Parcel")
    val PARCELABLE = ClassName.get("android.os", "Parcelable")
    val PARCELABLE_CREATOR = ClassName.get("android.os", "Parcelable", "Creator")

    val OBFUSCATION_UTIL = classNameOf<ObfuscationUtil>()

    /**
     * Gets the [TType] member name corresponding to the given type-code.
     *
     * @param code the code whose name is needed
     * @return the TType member name as a string
     */
    fun getTypeCodeName(code: Byte): String {
        return when(code) {
            TType.BOOL -> "BOOL"
            TType.BYTE -> "BYTE"
            TType.I16 -> "I16"
            TType.I32 -> "I32"
            TType.I64 -> "I64"
            TType.DOUBLE -> "DOUBLE"
            TType.STRING -> "STRING"
            TType.STRUCT -> "STRUCT"
            TType.LIST -> "LIST"
            TType.SET -> "SET"
            TType.MAP -> "MAP"
            TType.VOID -> "VOID"
            TType.STOP -> "STOP"
            else -> throw NoSuchElementException("not a TType member: $code")
        }
    }
}

enum class NullabilityAnnotationType(
    internal val notNullClassName: ClassName?,
    internal val nullableClassName: ClassName?
) {
    NONE(null, null),
    ANDROID_SUPPORT(
            notNullClassName = TypeNames.ANDROID_SUPPORT_NOT_NULL,
            nullableClassName = TypeNames.ANDROID_SUPPORT_NULLABLE
    ),
    ANDROIDX(
            notNullClassName = TypeNames.ANDROIDX_NOT_NULL,
            nullableClassName = TypeNames.ANDROIDX_NULLABLE
    )
}

internal inline fun <reified T> classNameOf(): ClassName = ClassName.get(T::class.java)
