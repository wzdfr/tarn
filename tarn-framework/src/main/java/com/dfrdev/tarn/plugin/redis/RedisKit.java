package com.dfrdev.tarn.plugin.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 静态操作门面（Tarn 2.0+）。
 * <p>
 * 覆盖缓存场景常用命令；复杂操作（pipeline/事务/Lua）可通过
 * {@link #getJedis()} 取原生连接自行处理（务必 try-with-resources 归还连接）。
 * <p>
 * 使用前需确保 {@link RedisPlugin} 已启动（YAML 配置 tarn.redis.enabled=true
 * 或手工注册插件），否则所有方法抛出 IllegalStateException。
 *
 * @author dfr
 * @see RedisPlugin
 * @since 2.0
 */
public final class RedisKit {

    private RedisKit() {
    }

    /**
     * 借用一个 Jedis 连接（用完自动归还）
     */
    public static Jedis getJedis() {
        JedisPool pool = RedisPlugin.getPool();
        if (pool == null) {
            throw new IllegalStateException("[RedisKit] Redis 未启用，请在 application.yml 配置 tarn.redis.enabled=true");
        }
        return pool.getResource();
    }

    // ---------- String ----------

    public static String get(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.get(key);
        } finally {
            jedis.close();
        }
    }

    public static void set(String key, String value) {
        Jedis jedis = getJedis();
        try {
            jedis.set(key, value);
        } finally {
            jedis.close();
        }
    }

    /**
     * set + 过期时间（秒）
     */
    public static void set(String key, String value, int expireSeconds) {
        Jedis jedis = getJedis();
        try {
            jedis.setex(key, expireSeconds, value);
        } finally {
            jedis.close();
        }
    }

    /**
     * 不存在才写入（分布式锁基础），成功返回 true
     */
    public static boolean setnx(String key, String value, int expireSeconds) {
        Jedis jedis = getJedis();
        try {
            String ret = jedis.set(key, value, new SetParams().nx().ex(expireSeconds));
            return "OK".equals(ret);
        } finally {
            jedis.close();
        }
    }

    // ---------- Key ----------

    public static Long del(String... keys) {
        Jedis jedis = getJedis();
        try {
            return jedis.del(keys);
        } finally {
            jedis.close();
        }
    }

    public static boolean exists(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.exists(key);
        } finally {
            jedis.close();
        }
    }

    /**
     * 设置过期时间（秒），返回是否成功
     */
    public static boolean expire(String key, int seconds) {
        Jedis jedis = getJedis();
        try {
            return jedis.expire(key, seconds) == 1L;
        } finally {
            jedis.close();
        }
    }

    public static Long ttl(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.ttl(key);
        } finally {
            jedis.close();
        }
    }

    public static Long incr(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.incr(key);
        } finally {
            jedis.close();
        }
    }

    // ---------- Hash ----------

    public static String hget(String key, String field) {
        Jedis jedis = getJedis();
        try {
            return jedis.hget(key, field);
        } finally {
            jedis.close();
        }
    }

    public static Map<String, String> hgetAll(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.hgetAll(key);
        } finally {
            jedis.close();
        }
    }

    public static void hset(String key, String field, String value) {
        Jedis jedis = getJedis();
        try {
            jedis.hset(key, field, value);
        } finally {
            jedis.close();
        }
    }

    public static Long hdel(String key, String... fields) {
        Jedis jedis = getJedis();
        try {
            return jedis.hdel(key, fields);
        } finally {
            jedis.close();
        }
    }

    // ---------- List ----------

    public static void lpush(String key, String... values) {
        Jedis jedis = getJedis();
        try {
            jedis.lpush(key, values);
        } finally {
            jedis.close();
        }
    }

    public static String rpop(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.rpop(key);
        } finally {
            jedis.close();
        }
    }

    public static List<String> lrange(String key, long start, long end) {
        Jedis jedis = getJedis();
        try {
            return jedis.lrange(key, start, end);
        } finally {
            jedis.close();
        }
    }

    // ---------- Set ----------

    public static void sadd(String key, String... members) {
        Jedis jedis = getJedis();
        try {
            jedis.sadd(key, members);
        } finally {
            jedis.close();
        }
    }

    public static Set<String> smembers(String key) {
        Jedis jedis = getJedis();
        try {
            return jedis.smembers(key);
        } finally {
            jedis.close();
        }
    }

    // ---------- Pub/Sub ----------

    public static void publish(String channel, String message) {
        Jedis jedis = getJedis();
        try {
            jedis.publish(channel, message);
        } finally {
            jedis.close();
        }
    }
}
