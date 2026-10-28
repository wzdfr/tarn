package examples.test.services.impl;

import com.dfrdev.tarn.aop.annotation.EService;
import com.dfrdev.tarn.core.db.AbstractService;
import examples.test.emtity.Hello;
import examples.test.services.HelloService;

/**
 * Created by dfr on 15-5-4.
 */
@EService("helloService")
public class HelloImpl extends AbstractService<Hello> implements HelloService {

}
