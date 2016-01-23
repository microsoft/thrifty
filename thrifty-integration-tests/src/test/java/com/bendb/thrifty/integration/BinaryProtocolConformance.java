/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.integration;

import com.bendb.thrifty.protocol.BinaryProtocol;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.testing.ServerProtocol;
import com.bendb.thrifty.testing.ServerTransport;
import com.bendb.thrifty.transport.Transport;

public class BinaryProtocolConformance extends ConformanceBase {
    @Override
    protected ServerTransport getServerTransport() {
        return ServerTransport.BLOCKING;
    }

    @Override
    protected ServerProtocol getServerProtocol() {
        return ServerProtocol.BINARY;
    }

    @Override
    protected Protocol createProtocol(Transport transport) {
        return new BinaryProtocol(transport);
    }
}
