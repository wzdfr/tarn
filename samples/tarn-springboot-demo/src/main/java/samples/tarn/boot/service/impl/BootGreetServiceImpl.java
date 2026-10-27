package samples.tarn.boot.service.impl;

import cc.ruixing.framework.tarn.aop.annotation.EService;
import samples.tarn.boot.service.BootGreetService;

/**
 * Tarn 服务实现：名字必须与控制器字段名一致（{@code bootGreetService}）。
 *
 * @author dfr
 * @since 2.1
 */
@EService("bootGreetService")
public class BootGreetServiceImpl implements BootGreetService {

    @Override
    public String greet(String name) {
        return "Hello, " + name + " (from Tarn MVC in Spring Boot)";
    }
}
