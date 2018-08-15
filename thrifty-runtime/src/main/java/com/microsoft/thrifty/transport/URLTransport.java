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
package com.microsoft.thrifty.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class URLTransport extends Transport {

    private URL url;
    private int timeout = 0;

    private HttpURLConnection connection;
    private InputStream inputStream;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public static class Builder {

        private URL url;
        private int timeout = 0;

        public Builder(URL url) {
            this.url = url;
        }

        public Builder setTimeout(int timeout) {
            if (timeout < 0) {
                throw new IllegalArgumentException("timeout can not be negative");
            }
            this.timeout = timeout;
            return this;
        }

        public URLTransport build() {
            return new URLTransport(this);
        }
    }

    URLTransport(Builder builder) {
        this.url = builder.url;
        this.timeout = builder.timeout;
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        return inputStream.read(buffer, offset, count);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        outputStream.write(buffer, offset, count);
    }

    @Override
    public void flush() throws IOException {

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        byte[] data = outputStream.toByteArray();
        outputStream.reset();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(timeout);
        connection.setDoOutput(true);

        connection.setRequestProperty("Accept", "application/x-thrift");
        connection.setRequestProperty("Content-Type", "application/x-thrift");

        connection.connect();

        connection.getOutputStream().write(data);
        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP response code not OK");
        }

        this.inputStream = connection.getInputStream();
    }

    @Override
    public void close() throws IOException {
        HttpURLConnection connection = this.connection;
        InputStream inputStream = this.inputStream;
        OutputStream outputStream = this.outputStream;

        this.connection = null;

        try {
            inputStream.close();
        } catch (Exception ignored) {
        }

        try {
            outputStream.close();
        } catch (Exception ignored) {
        }

        connection.disconnect();
    }
}
