package com.dfrdev.tarn.plugin.ehcache;

import com.dfrdev.tarn.util.LogUtil;
import org.apache.log4j.Logger;

/**
 * Created by dfr on 14-3-14.
 */
public class TarnCacheKit {
    private static volatile TarnSimpleCache simpleCache;
    private static final Logger log = Logger.getLogger(TarnCacheKit.class);

   public static void init() {
        simpleCache = new TarnSimpleCache();
       LogUtil.debug("TarnCache Plugin 初始化");
    }

    public static void init(int maxCapacity) {
        simpleCache = new TarnSimpleCache(maxCapacity);
    }

    public  static void init(TarnSimpleCache simpleCache) {
        TarnCacheKit.simpleCache = simpleCache;
    }


    static Object getOrAddCache(String key) {
        Object cache = simpleCache.get(key);
        if (cache == null) {
            synchronized (simpleCache) {
                cache = simpleCache.get(key);
                if (cache == null) {
                    log.warn("找不到缓存配置 [" + key + "], using default.");
                    simpleCache.put(key, null);
                    cache = simpleCache.get(key);
                    // log.debug("缓存 [" + cacheName + "] 开始.");
                }
            }
        }
        return cache;
    }

    public static Object get(Object key) {
        if (simpleCache == null) {
            log.warn("simpleCache未初始化成功");
        }
        return simpleCache.get(key);

    }

    public static void put(Object key, Object value) {
        if (simpleCache == null) {
            log.warn("simpleCache未初始化成功");
        }
        simpleCache.put(key, value);

    }

}
