package com.bendb.thrifty.service;

/**
 *
 * @param <T>
 */
public interface ServiceMethodCallback<T> {
    void onSuccess(T result);
    void onError(Throwable error);
}
