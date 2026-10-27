package cc.ruixing.framework.tarn.plugin.mybatis;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * MyBatis 统一门面（Tarn 2.0+）。
 * <p>
 * 使用前提：已通过 {@code PluginManager.me().register(new MybatisPlugin())}
 * 或 {@code application.yml} 的 {@code tarn.mybatis.enabled: true} 启动插件。
 * <pre>
 *     // 方式一：直接获取 Mapper 代理（推荐，绑定当前线程会话，请求结束时由框架自动关闭）
 *     DemoUserMapper mapper = MybatisKit.getMapper(DemoUserMapper.class);
 *     DemoUser user = mapper.selectById(1L);
 *
 *     // 方式二：手动管理 Session（跨请求事务场景）
 *     SqlSession session = MybatisKit.openSession(false); // 手动提交
 *     try {
 *         session.getMapper(DemoUserMapper.class).insert(user);
 *         session.commit();
 *     } finally {
 *         session.close();
 *     }
 * </pre>
 * 会话生命周期说明：
 * <ul>
 *     <li>{@link #getMapper(Class)} 使用线程级会话（自动提交），
 *     在 Web 请求结束时由 {@code TarnDispatcherFilter} 调用 {@link #close()} 释放</li>
 *     <li>非 Web 线程（定时任务/后台线程）调用后请自行 {@link #close()}，避免连接泄漏</li>
 * </ul>
 *
 * @author dfr
 * @see MybatisPlugin
 * @since 2.0
 */
public final class MybatisKit {

    private static final ThreadLocal<SqlSession> SESSION_HOLDER = new ThreadLocal<SqlSession>();

    private MybatisKit() {
    }

    private static SqlSessionFactory factory() {
        SqlSessionFactory f = MybatisPlugin.getSqlSessionFactory();
        if (f == null) {
            throw new IllegalStateException(
                    "MybatisPlugin 尚未启动：请配置 tarn.mybatis.enabled=true，"
                            + "或在 TarnConfig.configPlugin 中注册 plugins.add(new MybatisPlugin())");
        }
        return f;
    }

    /**
     * 打开自动提交的 SqlSession（调用方负责 close）
     */
    public static SqlSession openSession() {
        return factory().openSession(true);
    }

    /**
     * 打开 SqlSession（调用方负责 close）
     *
     * @param autoCommit true 自动提交；false 手动 commit/rollback
     */
    public static SqlSession openSession(boolean autoCommit) {
        return factory().openSession(autoCommit);
    }

    /**
     * 获取当前线程绑定的会话（不存在则创建，自动提交）。
     * <p>
     * 首次获取 Mapper 时自动向 MyBatis 注册该 Mapper 接口，
     * 因此无需事先在 mybatis-config.xml 中声明，也无需编写 XML。
     */
    public static SqlSession currentSession() {
        SqlSession session = SESSION_HOLDER.get();
        if (session == null) {
            session = factory().openSession(true);
            SESSION_HOLDER.set(session);
        }
        return session;
    }

    /**
     * 获取 Mapper 代理（绑定当前线程会话，请勿在请求外长期持有）
     */
    public static <T> T getMapper(Class<T> mapperInterface) {
        SqlSessionFactory f = factory();
        if (!f.getConfiguration().hasMapper(mapperInterface)) {
            synchronized (MybatisKit.class) {
                if (!f.getConfiguration().hasMapper(mapperInterface)) {
                    f.getConfiguration().addMapper(mapperInterface);
                }
            }
        }
        return currentSession().getMapper(mapperInterface);
    }

    /**
     * 释放当前线程会话。Web 请求结束时由框架自动调用，非 Web 线程请手动调用。
     */
    public static void close() {
        SqlSession session = SESSION_HOLDER.get();
        if (session != null) {
            SESSION_HOLDER.remove();
            session.close();
        }
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return factory();
    }
}
