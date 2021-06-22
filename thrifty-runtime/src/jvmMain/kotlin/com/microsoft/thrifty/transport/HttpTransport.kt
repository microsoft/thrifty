/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.microsoft.thrifty.transport


import com.microsoft.thrifty.internal.ProtocolException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP implementation of the TTransport interface. Used for working with a
 * Thrift web services implementation (using for example TServlet).
 *
 * THIS IMPLEMENTATION IS NOT THREAD-SAFE !!!
 *
 * Based on the official thrift java THttpTransport with the apache client support removed.
 * Both due to wanting to avoid the additional dependency as well as it being a bit weird to have two
 * implementations to switch between in the same class.
 *
 * Uses HttpURLConnection internally
 *
 * Also note that under high load, the HttpURLConnection implementation
 * may exhaust the open file descriptor limit.
 *
 * @see [THRIFT-970](https://issues.apache.org/jira/browse/THRIFT-970)
 */
open class HttpTransport(url: String) : Transport {
    private val url: URL = URL(url)
    private var currentState: Transport = Writing()
    private var connectTimeout: Int? = null
    private var readTimeout: Int? = null
    private val customHeaders = mutableMapOf<String, String>()
    private val sendBuffer = ByteArrayOutputStream()

    private inner class Writing : Transport {
        override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
            throw ProtocolException("Currently in writing state")
        }

        override fun write(buffer: ByteArray, offset: Int, count: Int) {
            sendBuffer.write(buffer, offset, count)
        }

        override fun flush() {
            val bytesToSend = sendBuffer.toByteArray()
            sendBuffer.reset()
            send(bytesToSend)
        }

        override fun close() {
            // do nothing
        }
    }

    private inner class Reading(val inputStream: InputStream) : Transport {
        override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
            val ret = inputStream.read(buffer, offset, count)
            if (ret == -1) {
                throw ProtocolException("No more data available.")
            }
            return ret
        }

        override fun write(buffer: ByteArray, offset: Int, count: Int) {
            throw ProtocolException("currently in reading state")
        }

        override fun flush() {
            throw ProtocolException("currently in reading state")
        }

        override fun close() {
            inputStream.close()
        }
    }

    fun send(data: ByteArray) {
        // Create connection object
        val connection = url.openConnection() as HttpURLConnection

        prepareConnection(connection)
        // Make the request
        connection.connect()
        connection.outputStream.write(data)
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw ProtocolException("HTTP Response code: $responseCode")
        }

        // Read the response
        this.currentState = Reading(connection.inputStream)
    }

    protected open fun prepareConnection(connection: HttpURLConnection) {
        // Timeouts, only if explicitly set
        connectTimeout?.let { connection.connectTimeout = it }
        readTimeout?.let { connection.readTimeout = it }

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-thrift")
        connection.setRequestProperty("Accept", "application/x-thrift")
        connection.setRequestProperty("User-Agent", "Java/THttpClient")
        for ((key, value) in customHeaders) {
            connection.setRequestProperty(key, value)
        }
        connection.doOutput = true
    }

    fun setConnectTimeout(timeout: Int) {
        connectTimeout = timeout
    }

    fun setReadTimeout(timeout: Int) {
        readTimeout = timeout
    }

    fun setCustomHeaders(headers: Map<String, String>) {
        customHeaders.clear()
        customHeaders.putAll(headers)
    }

    fun setCustomHeader(key: String, value: String) {
        customHeaders[key] = value
    }

    override fun close() {
        currentState.close()
    }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int = currentState.read(buffer, offset, count)

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        // this mirrors the original behaviour, though it is not very elegant.
        // we don't know when the user is done reading, so when they start writing again,
        // we just go with it.
        if (currentState is Reading) {
            currentState.close()
            currentState = Writing()
        }
        currentState.write(buffer, offset, count)
    }

    override fun flush() {
        currentState.flush()
    }
}
