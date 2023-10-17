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
package com.microsoft.thrifty.transport

import com.microsoft.thrifty.protocol.BinaryProtocol
import com.microsoft.thrifty.protocol.Xtruct
import io.kotest.matchers.shouldBe
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import okio.use
import platform.Network.nw_connection_set_queue
import platform.Network.nw_connection_set_state_changed_handler
import platform.Network.nw_connection_start
import platform.Network.nw_connection_state_cancelled
import platform.Network.nw_connection_state_failed
import platform.Network.nw_connection_state_ready
import platform.Network.nw_listener_cancel
import platform.Network.nw_listener_create
import platform.Network.nw_listener_get_port
import platform.Network.nw_listener_set_new_connection_handler
import platform.Network.nw_listener_set_queue
import platform.Network.nw_listener_set_state_changed_handler
import platform.Network.nw_listener_start
import platform.Network.nw_listener_state_cancelled
import platform.Network.nw_listener_state_failed
import platform.Network.nw_listener_state_ready
import platform.Network.nw_parameters_copy_default_protocol_stack
import platform.Network.nw_parameters_create
import platform.Network.nw_protocol_stack_set_transport_protocol
import platform.Network.nw_tcp_create_options
import platform.Network.nw_tcp_options_set_connection_timeout
import platform.Network.nw_tcp_options_set_enable_keepalive
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.posix.QOS_CLASS_DEFAULT
import kotlin.test.Test

@OptIn(ExperimentalForeignApi::class)
class NwSocketTest {
    @Test
    fun canRoundTripStructs() {
        val xtruct = Xtruct.Builder()
            .bool_thing(true)
            .byte_thing(1)
            .i32_thing(2)
            .i64_thing(3)
            .double_thing(4.0)
            .string_thing("five")
            .build()

        val globalQueue = dispatch_get_global_queue(QOS_CLASS_DEFAULT.convert(), 0.convert())

        // For some reason, NW_PARAMETERS_DISABLE_PROTOCOL wasn't actually disabling TLS
        // on the listener; we'd see "handshake failed" errors.  Who even knows.
        // Manually creating parameters, and not even touching TLS, seems to work.
        //val parameters = nw_parameters_create_secure_tcp(NW_PARAMETERS_DISABLE_PROTOCOL, NW_PARAMETERS_DEFAULT_CONFIGURATION)

        val tcpOptions = nw_tcp_create_options()
        nw_tcp_options_set_enable_keepalive(tcpOptions, true)
        nw_tcp_options_set_connection_timeout(tcpOptions, 60.convert())

        val parameters = nw_parameters_create()
        val stack = nw_parameters_copy_default_protocol_stack(parameters)
        nw_protocol_stack_set_transport_protocol(stack, tcpOptions)

        val serverListener = nw_listener_create(parameters)
        nw_listener_set_queue(serverListener, globalQueue)
        nw_listener_set_new_connection_handler(serverListener) { connection ->
            nw_connection_set_state_changed_handler(connection) { state, err ->
                if (state == nw_connection_state_ready) {
                    val transport = SocketTransport(connection)
                    val protocol = BinaryProtocol(transport)
                    xtruct.write(protocol)
                } else if (state in listOf(
                        nw_connection_state_failed,
                        nw_connection_state_cancelled
                    )
                ) {
                    println("server: I AM NOT READY")
                }
            }

            nw_connection_set_queue(connection, globalQueue)
            nw_connection_start(connection)
        }

        val readySem = dispatch_semaphore_create(0)
        var ready = false
        nw_listener_set_state_changed_handler(serverListener) { state, err ->
            if (state == nw_listener_state_ready) {
                ready = true
            }

            if (state in listOf(
                    nw_listener_state_ready,
                    nw_listener_state_failed,
                    nw_listener_state_cancelled
                )
            ) {
                dispatch_semaphore_signal(readySem)
            }
        }
        nw_listener_start(serverListener)
        dispatch_semaphore_wait(readySem, DISPATCH_TIME_FOREVER)

        if (!ready) {
            nw_listener_cancel(serverListener)
            throw AssertionError("Failed to set up a listener")
        }

        val clientSem = dispatch_semaphore_create(0)
        val clientQueue = dispatch_queue_create("client", null)
        var matched = false
        dispatch_async(clientQueue) {
            try {
                val port = nw_listener_get_port(serverListener)
                SocketTransport.Builder("127.0.0.1", port.toInt()).readTimeout(100).build()
                    .use { transport ->
                        transport.connect()
                        val protocol = BinaryProtocol(transport)
                        val readXtruct = Xtruct.ADAPTER.read(protocol)

                        if (readXtruct == xtruct) {
                            // Assertion errors don't make it out of dispatch queues,
                            // so we'll just set a flag and check it later.
                            matched = true
                        }
                    }
            } finally {
                nw_listener_cancel(serverListener)
                dispatch_semaphore_signal(clientSem)
            }
        }
        dispatch_semaphore_wait(clientSem, DISPATCH_TIME_FOREVER)

        matched shouldBe true
    }
}
