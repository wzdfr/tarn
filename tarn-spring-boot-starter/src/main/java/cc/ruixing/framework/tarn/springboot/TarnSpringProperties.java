package cc.ruixing.framework.tarn.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot 侧专属配置（Tarn 2.1+）。
 * <p>
 * 与 Tarn 自身的 {@code tarn.*} 配置（datasource/mvc/server 等）互不干扰：
 * Tarn 仍直接读取 classpath 下的 {@code application.yml}，本类只负责“如何在 Spring Boot 中注册 Tarn 过滤器”。
 * <p>
 * 示例（application.yml）：
 * <pre>
 * tarn:
 *   spring:
 *     enabled: true
 *     config-class: examples.boot.BootTarnConfig
 *     url-patterns:
 *       - /tarn/*
 *   datasource:        # 仍然由 Tarn 自己读取
 *     type: MYSQL
 *     url: jdbc:mysql://127.0.0.1:3306/demo
 *     username: root
 *     password:
 *     driver-class-name: com.mysql.cj.jdbc.Driver
 * </pre>
 *
 * @author dfr
 * @since 2.1
 */
@ConfigurationProperties(prefix = TarnSpringProperties.PREFIX)
public class TarnSpringProperties {

    public static final String PREFIX = "tarn.spring";

    /**
     * 是否启用 Tarn MVC（默认 true，设置 false 可完全关闭并让 Spring MVC 独占）
     */
    private boolean enabled = true;

    /**
     * TarnConfig 实现类全名（等同 web.xml 中 TarnDispatcherFilter 的 configClass 初始化参数）
     */
    private String configClass;

    /**
     * Tarn 拦截的 URL 模式。默认 /*，与 Spring MVC 共存时建议收敛成 /tarn/* 等前缀
     */
    private String[] urlPatterns = new String[]{"/*"};

    /**
     * 过滤器顺序。默认比 Spring 的编码/请求包装过滤器更靠前
     */
    private int order = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigClass() {
        return configClass;
    }

    public void setConfigClass(String configClass) {
        this.configClass = configClass;
    }

    public String[] getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(String[] urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
