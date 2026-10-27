package cc.ruixing.framework.tarn.plugin.redis;

import cc.ruixing.framework.tarn.config.YamlConfigKit;
import cc.ruixing.framework.tarn.plugin.Plugin;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.StringUtil;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis 插件（Tarn 2.0+）。
 * <p>
 * 配置方式（application.yml，无需 Java 代码即可启用）：
 * <pre>
 * tarn:
 *   redis:
 *     enabled: true
 *     host: 127.0.0.1
 *     port: 6379
 *     password:
 *     database: 0
 *     timeout: 2000
 *     pool:
 *       max-total: 32
 *       max-idle: 8
 *       min-idle: 0
 *       max-wait-millis: 3000
 * </pre>
 * 也可编程式注册：
 * <pre>
 *     PluginManager.me().register(new RedisPlugin());
 *     // 业务代码：
 *     RedisKit.set("key", "value", 3600);
 * </pre>
 * 依赖 jedis 3.8.0 + commons-pool2（本工程已内置）。
 *
 * @author dfr
 * @see RedisKit
 * @since 2.0
 */
public class RedisPlugin implements Plugin {

    /** YAML 配置键前缀 */
    public static final String CONF_PREFIX = "tarn.redis";

    private static volatile JedisPool pool;

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public int order() {
        // 缓存类插件在业务插件之前启动
        return -50;
    }

    @Override
    public void onStart() {
        if (pool != null) {
            return;
        }
        String host = YamlConfigKit.get(CONF_PREFIX + ".host", "127.0.0.1");
        int port = YamlConfigKit.getInt(CONF_PREFIX + ".port", 6379);
        String password = YamlConfigKit.get(CONF_PREFIX + ".password", "");
        int database = YamlConfigKit.getInt(CONF_PREFIX + ".database", 0);
        int timeout = YamlConfigKit.getInt(CONF_PREFIX + ".timeout", 2000);

        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<Jedis>();
        poolConfig.setMaxTotal(YamlConfigKit.getInt(CONF_PREFIX + ".pool.max-total", 32));
        poolConfig.setMaxIdle(YamlConfigKit.getInt(CONF_PREFIX + ".pool.max-idle", 8));
        poolConfig.setMinIdle(YamlConfigKit.getInt(CONF_PREFIX + ".pool.min-idle", 0));
        poolConfig.setMaxWaitMillis(YamlConfigKit.getInt(CONF_PREFIX + ".pool.max-wait-millis", 3000));
        poolConfig.setTestOnBorrow(YamlConfigKit.getBool(CONF_PREFIX + ".pool.test-on-borrow", true));

        synchronized (RedisPlugin.class) {
            if (pool == null) {
                pool = new JedisPool(poolConfig, host, port, timeout,
                        StringUtil.isEmpty(password) ? null : password, database);
            }
        }
        // 启动即连通性自检，失败直接抛出中断启动（避免运行期才发现配置错误）
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            jedis.ping();
            LogUtil.info("[RedisPlugin] Redis 连接成功 " + host + ":" + port + " db=" + database);
        } catch (Exception e) {
            onStop();
            throw new RuntimeException("[RedisPlugin] Redis 连接失败 " + host + ":" + port + " -> " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public void onStop() {
        if (pool != null) {
            synchronized (RedisPlugin.class) {
                if (pool != null) {
                    pool.destroy();
                    pool = null;
                }
            }
        }
    }

    /**
     * 获取连接池（未启动返回 null）
     */
    public static JedisPool getPool() {
        return pool;
    }

    /**
     * 是否已启用并启动
     */
    public static boolean isAvailable() {
        return pool != null;
    }
}
