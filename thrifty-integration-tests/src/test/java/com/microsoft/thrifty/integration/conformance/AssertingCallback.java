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

import com.microsoft.thrifty.service.ServiceMethodCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A convenience class for testing Thrifty remote method calls.
 *
 * <p>Allows for blocking, checking results, timing out, and failing in a
 * test-friendly way with AssertionErrors.
 */
public class AssertingCallback<T> implements ServiceMethodCallback<T> {
    private T result;
    private Throwable error;

    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void onSuccess(T result) {
        this.result = result;
        latch.countDown();
    }

    @Override
    public void onError(Throwable error) {
        this.error = error;
        latch.countDown();
    }

    public T getResult() throws Throwable {
        await();

        if (error != null) {
            throw error;
        }

        return result;
    }

    public Throwable getError() {
        await();

        return error;
    }

    private void await() {
        try {
            if (!latch.await(20000, TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Client callback timed out after 2 seconds");
            }

        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }
}
