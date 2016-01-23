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
package com.bendb.thrifty.service;

/**
 * Defines the means by which service-call results are communicated back to the
 * caller.
 *
 * @param <T> the type of result expected, or {@link Void}.
 */
public interface ServiceMethodCallback<T> {
    /**
     * Invoked upon successful return from a service call with the result, if
     * any.
     *
     * @param result The method result.  Will be {@code null} only if {@link T}
     *               is {@link Void}.
     */
    void onSuccess(T result);

    /**
     * Invoked upon a failure to complete the call, for any reason.
     *
     * @param error The {@linkplain Throwable} instance indicating the cause of
     *              failure.  It will be one of the declared exceptions of the
     *              service method, <em>or</em> an {@link java.io.IOException}
     *              detailing the IO failure, <em>or</em> a
     *              {@link java.net.ProtocolException} if an incomprehensible
     *              server response is received.
     */
    void onError(Throwable error);
}
