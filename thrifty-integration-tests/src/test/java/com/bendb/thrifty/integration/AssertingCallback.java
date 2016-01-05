/*
 * Copyright (C) 2015 Benjamin Bader
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

import com.bendb.thrifty.service.ServiceMethodCallback;

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

    public void await() {
        try {
            latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new AssertionError("Client callback timed out after 2 seconds");
        }
    }

    public T getResult() throws Throwable {
        if (error != null) {
            throw error;
        }

        return result;
    }

    public Throwable getError() {
        return error;
    }
}
