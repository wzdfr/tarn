package samples.tarn.boot.service;

/**
 * Tarn 服务层接口（由 {@code @EAutowired} 按字段名注入实现）。
 *
 * @author dfr
 * @since 2.1
 */
public interface BootGreetService {

    String greet(String name);
}
