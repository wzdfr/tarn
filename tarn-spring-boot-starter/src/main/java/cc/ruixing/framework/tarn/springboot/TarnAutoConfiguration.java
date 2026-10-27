package cc.ruixing.framework.tarn.springboot;

import cc.ruixing.framework.tarn.core.TarnDispatcherFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Tarn MVC 在 Spring Boot 中的自动装配（Tarn 2.1+）。
 * <p>
 * 原理：Spring Boot 容器里注册 Tarn 的 {@link TarnDispatcherFilter}，Tarn 自己完成
 * 路由表构建 + 服务注入 + 请求分发；未命中 Tarn 路由的请求会由 {@code filterChain.doFilter}
 * 继续交给 Spring MVC 的 {@code DispatcherServlet}，两者并行共存。
 * <p>
 * 使用方式（三步）：
 * <ol>
 *     <li>引入依赖 {@code cc.ruixing.framework:tarn-spring-boot-starter}</li>
 *     <li>classpath 放 {@code context-mapping.xml}（声明控制器/服务包）与 {@code application.yml}</li>
 *     <li>启动类加 {@code @SpringBootApplication}，无需写任何额外配置</li>
 * </ol>
 *
 * @author dfr
 * @since 2.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "javax.servlet.Filter")
@ConditionalOnProperty(prefix = TarnSpringProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TarnSpringProperties.class)
public class TarnAutoConfiguration {

    /**
     * 注册 Tarn 主调度过滤器。
     * <p>
     * 注意：不要再用 {@code @Bean} 直接暴露 TarnDispatcherFilter，
     * 否则 Spring Boot 会把它注册成全局过滤器（等价 urlPatterns=/*、order 最低）。
     */
    @Bean
    @ConditionalOnMissingBean(name = "tarnDispatcherFilterRegistration")
    public FilterRegistrationBean<TarnDispatcherFilter> tarnDispatcherFilterRegistration(
            TarnSpringProperties properties) {
        FilterRegistrationBean<TarnDispatcherFilter> registration =
                new FilterRegistrationBean<TarnDispatcherFilter>(new TarnDispatcherFilter());
        registration.setName("tarnDispatcherFilter");
        registration.setOrder(properties.getOrder());
        registration.setUrlPatterns(java.util.Arrays.asList(properties.getUrlPatterns()));
        registration.addInitParameter("asyncSupported", "false");
        if (StringUtils.hasText(properties.getConfigClass())) {
            registration.addInitParameter("configClass", properties.getConfigClass());
        }
        return registration;
    }
}
