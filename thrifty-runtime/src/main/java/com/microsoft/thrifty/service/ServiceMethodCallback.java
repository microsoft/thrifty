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
package com.microsoft.thrifty.service;

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
     * <p>The {@code error} parameter will be one of:
     * <ul>
     *     <li>
     *         An the exception declared in the service's Thrift IDL.
     *     </li>
     *     <li>
     *         An {@link java.io.IOException} detailing an IO failure.
     *     </li>
     *     <li>
     *         A {@link java.net.ProtocolException} if a malformed response is
     *         received.
     *     </li>
     *     <li>
     *         An {@link java.util.concurrent.CancellationException} if the
     *         client is shut down before the method could be executed.
     *     </li>
     * </ul>
     *
     * @param error The {@linkplain Throwable} instance indicating the cause of
     *              failure.
     */
    void onError(Throwable error);
}
