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
package com.microsoft.thrifty.integration.conformance;

import com.microsoft.thrifty.protocol.CompactProtocol;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.testing.ServerProtocol;
import com.microsoft.thrifty.testing.ServerTransport;
import com.microsoft.thrifty.transport.FramedTransport;
import com.microsoft.thrifty.transport.Transport;

public class NonblockingCompactProtocolSyncConformance extends SyncConformanceBase {
    @Override
    protected ServerTransport getServerTransport() {
        return ServerTransport.NON_BLOCKING;
    }

    @Override
    protected ServerProtocol getServerProtocol() {
        return ServerProtocol.COMPACT;
    }

    @Override
    protected Transport decorateTransport(Transport transport) {
        return new FramedTransport(transport);
    }

    @Override
    protected Protocol createProtocol(Transport transport) {
        return new CompactProtocol(transport);
    }
}
