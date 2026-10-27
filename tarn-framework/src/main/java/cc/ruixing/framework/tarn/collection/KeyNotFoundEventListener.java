package cc.ruixing.framework.tarn.collection;

public abstract interface KeyNotFoundEventListener<K, V> {
    public abstract V onKeyNotFound(K paramK);
}

