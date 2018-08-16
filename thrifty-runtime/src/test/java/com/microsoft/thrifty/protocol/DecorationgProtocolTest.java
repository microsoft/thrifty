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
package com.microsoft.thrifty.protocol;

import com.microsoft.thrifty.transport.BufferTransport;
import com.microsoft.thrifty.transport.Transport;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static com.microsoft.thrifty.TType.I32;
import static com.microsoft.thrifty.TType.STRING;
import static com.microsoft.thrifty.service.TMessageType.CALL;
import static okio.ByteString.encodeUtf8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public final class DecorationgProtocolTest {

    private Protocol mockProtocol;
    private Transport mockTransport;
    private DecoratingProtocol decoratingProtocol;

    @Before
    public void setup() {
        mockProtocol = mock(Protocol.class);
        mockTransport = mock(Transport.class);
        decoratingProtocol = new DecoratingProtocol(mockProtocol, mockTransport) {};
    }

    @Test
    public void testCtor() {
        Protocol binaryProtocol = new BinaryProtocol(new BufferTransport());
        Protocol decoratingProtocol = new DecoratingProtocol(binaryProtocol) {};
        assertThat(decoratingProtocol.transport).isSameAs(binaryProtocol.transport);
    }

    @Test
    public void testWriteMessageBegin() throws IOException {
        decoratingProtocol.writeMessageBegin("method", CALL, 10);
        verify(mockProtocol).writeMessageBegin("method", CALL, 10);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }
    
    @Test
    public void testWriteMessageEnd() throws IOException {
        decoratingProtocol.writeMessageEnd();
        verify(mockProtocol).writeMessageEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteStructBegin() throws IOException {
        decoratingProtocol.writeStructBegin("struct");
        verify(mockProtocol).writeStructBegin("struct");
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteStructEnd() throws IOException {
        decoratingProtocol.writeStructEnd();
        verify(mockProtocol).writeStructEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteFieldBegin() throws IOException {
        decoratingProtocol.writeFieldBegin("some_field", 9, I32);
        verify(mockProtocol).writeFieldBegin("some_field", 9, I32);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteFieldEnd() throws IOException {
        decoratingProtocol.writeFieldEnd();
        verify(mockProtocol).writeFieldEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }


    @Test
    public void testWriteFieldStop() throws IOException {
        decoratingProtocol.writeFieldStop();
        verify(mockProtocol).writeFieldStop();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteMapBegin() throws IOException {
        decoratingProtocol.writeMapBegin(STRING, I32, 1);
        verify(mockProtocol).writeMapBegin(STRING, I32, 1);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteMapEnd() throws IOException {
        decoratingProtocol.writeMapEnd();
        verify(mockProtocol).writeMapEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteListBegin() throws IOException {
        decoratingProtocol.writeListBegin(STRING, 3);
        verify(mockProtocol).writeListBegin(STRING, 3);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }


    @Test
    public void testWriteListEnd() throws IOException {
        decoratingProtocol.writeListEnd();
        verify(mockProtocol).writeListEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteSetBegin() throws IOException {
        decoratingProtocol.writeSetBegin(STRING, 1);
        verify(mockProtocol).writeSetBegin(STRING, 1);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteSetEnd() throws IOException {
        decoratingProtocol.writeSetEnd();
        verify(mockProtocol).writeSetEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteBool() throws IOException {
        decoratingProtocol.writeBool(true);
        verify(mockProtocol).writeBool(true);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteByte() throws IOException {
        byte b = 100;
        decoratingProtocol.writeByte(b);
        verify(mockProtocol).writeByte(b);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteI16() throws IOException {
        short i16 = 10000;
        decoratingProtocol.writeI16(i16);
        verify(mockProtocol).writeI16(i16);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteI32() throws IOException {
        decoratingProtocol.writeI32(100000);
        verify(mockProtocol).writeI32(100000);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteI64() throws IOException {
        decoratingProtocol.writeI64(10000000000L);
        verify(mockProtocol).writeI64(10000000000L);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteDouble() throws IOException {
        decoratingProtocol.writeDouble(0.1);
        verify(mockProtocol).writeDouble(0.1);
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteString() throws IOException {
        decoratingProtocol.writeString("hello");
        verify(mockProtocol).writeString("hello");
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testWriteBinary() throws IOException {
        decoratingProtocol.writeBinary(encodeUtf8("hello"));
        verify(mockProtocol).writeBinary(encodeUtf8("hello"));
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadMessageBegin() throws IOException {
        decoratingProtocol.readMessageBegin();
        verify(mockProtocol).readMessageBegin();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadMessageEnd() throws IOException {
        decoratingProtocol.readMessageEnd();
        verify(mockProtocol).readMessageEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadStructBegin() throws IOException {
        decoratingProtocol.readStructBegin();
        verify(mockProtocol).readStructBegin();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadStructEnd() throws IOException {
        decoratingProtocol.readStructEnd();
        verify(mockProtocol).readStructEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }
    
    @Test
    public void testReadFieldBegin() throws IOException {
        decoratingProtocol.readFieldBegin();
        verify(mockProtocol).readFieldBegin();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadFieldEnd() throws IOException {
        decoratingProtocol.readFieldEnd();
        verify(mockProtocol).readFieldEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }
    
    @Test
    public void testReadMapBegin() throws IOException {
        decoratingProtocol.readMapBegin();
        verify(mockProtocol).readMapBegin();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadMapEnd() throws IOException {
        decoratingProtocol.readMapEnd();
        verify(mockProtocol).readMapEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadListBegin() throws IOException {
        decoratingProtocol.readListBegin();
        verify(mockProtocol).readListBegin();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadListEnd() throws IOException {
        decoratingProtocol.readListEnd();
        verify(mockProtocol).readListEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadSetBegin() throws IOException {
        decoratingProtocol.readSetBegin();
        verify(mockProtocol).readSetBegin();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadSetEnd() throws IOException {
        decoratingProtocol.readSetEnd();
        verify(mockProtocol).readSetEnd();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadBool() throws IOException {
        decoratingProtocol.readBool();
        verify(mockProtocol).readBool();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadByte() throws IOException {
        decoratingProtocol.readByte();
        verify(mockProtocol).readByte();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadI16() throws IOException {
        decoratingProtocol.readI16();
        verify(mockProtocol).readI16();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadI32() throws IOException {
        decoratingProtocol.readI32();
        verify(mockProtocol).readI32();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadI64() throws IOException {
        decoratingProtocol.readI64();
        verify(mockProtocol).readI64();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadDouble() throws IOException {
        decoratingProtocol.readDouble();
        verify(mockProtocol).readDouble();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadString() throws IOException {
        decoratingProtocol.readString();
        verify(mockProtocol).readString();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReadBinary() throws IOException {
        decoratingProtocol.readBinary();
        verify(mockProtocol).readBinary();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }


    @Test
    public void testFlush() throws IOException {
        decoratingProtocol.flush();
        verify(mockProtocol).flush();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testReset() throws IOException {
        decoratingProtocol.reset();
        verify(mockProtocol).reset();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }

    @Test
    public void testClose() throws IOException {
        decoratingProtocol.close();
        verify(mockProtocol).close();
        verifyNoMoreInteractions(mockProtocol);
        verifyZeroInteractions(mockTransport);
    }
}
