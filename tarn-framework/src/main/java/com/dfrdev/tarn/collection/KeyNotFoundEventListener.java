package com.dfrdev.tarn.collection;

public abstract interface KeyNotFoundEventListener<K, V> {
    public abstract V onKeyNotFound(K paramK);
}

