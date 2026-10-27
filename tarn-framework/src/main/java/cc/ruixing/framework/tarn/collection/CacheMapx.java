package cc.ruixing.framework.tarn.collection;

import cc.ruixing.framework.tarn.aop.Func0;

import java.util.Map;

public final class CacheMapx<K, V> extends Mapx<K, V> {
    private static final long serialVersionUID = 201404182029L;
    final int maxCapacity;
    private ExitEventListener<K, V> exitListener;

    public CacheMapx() {
        super(true);
        this.maxCapacity = 2147483615;
        this.accessCount = 0;
    }

    public CacheMapx(int maxCapacity) {
        super(true);
        if (maxCapacity > 2147483615) {
            maxCapacity = 2147483615;
        }
        this.maxCapacity = maxCapacity;
        this.accessCount = 0;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (this.size >= this.maxCapacity + 32) {
            this.wlock.lock();
            try {
                for (int i = 0; i < 32; i++) {
                    Mapx.Entry min = null;
                    for (Map.Entry entry : entrySet()) {
                        Mapx.Entry e = (Mapx.Entry) entry;
                        if ((min == null) || (min.lastAccess >= e.lastAccess)) {
                            min = e;
                        }
                    }
                    removeMapping(min);
                    if (this.exitListener != null)
                        this.exitListener.onExit((K)min.getKey(), (V)min.getValue());
                }
            } finally {
                this.wlock.unlock();
            }
        }
        return false;
    }

    public void setExitEventListener(ExitEventListener<K, V> listener) {
        this.exitListener = listener;
    }

    /**
     * 从缓存中获得对象，当对象不在缓存中或已经过期返回Func0回调产生的对象
     *
     * @param key      键
     * @param supplier 如果不存在回调方法，用于生产值对象
     * @return 值对象
     */
    public V get(K key, Func0<V> supplier) {
        V v = get(key);

        if(null == v && null != supplier){
            SHAM_LOCK.lock();
            try{
                v = get(key);
                // 双重检查，防止在竞争锁的过程中已经有其它线程写入
                if (null == v) {
                    try {
                        v = supplier.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                   put(key, v);
                }
            } finally{
                SHAM_LOCK.unlock();
            }
        }

        return v;
    }
}

