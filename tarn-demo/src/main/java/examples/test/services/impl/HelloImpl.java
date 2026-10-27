package examples.test.services.impl;

import cc.ruixing.framework.tarn.aop.annotation.EService;
import cc.ruixing.framework.tarn.core.AbstractService;
import examples.test.emtity.Hello;
import examples.test.services.HelloService;

/**
 * Created by dfr on 15-5-4.
 */
@EService("helloService")
public class HelloImpl extends AbstractService<Hello> implements HelloService {

}
