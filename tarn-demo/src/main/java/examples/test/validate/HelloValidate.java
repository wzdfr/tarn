package examples.test.validate;

import com.dfrdev.tarn.core.Controller;
import com.dfrdev.tarn.validate.Validator;
import com.alibaba.fastjson.JSON;

/**
 * Created by dfr on 15-5-5.
 */
public class HelloValidate extends Validator {

    /**
     * Use validateXxx method to validate the parameters of this action.
     *
     * @param c
     */
    @Override
    protected void validate(Controller c) {
        validateRequiredString("name", "nameMsg", "请输入权限名称!");
        validateInteger("hello.id", "idMsg", "hello.id不是正确的数值型");
        validateEmail("hello.org.name", "orgNameMsg", "不是正确的邮箱");;
        System.out.println("验证结束");
    }

    /**
     * Handle the validate error.
     * Example:<br>
     * controller.keepPara();<br>
     * controller.renderFreeMarker("register.html");
     *
     * @param c
     */
    @Override
    protected void handleError(Controller controller) {
       System.out.println(getActionInfo().getMethodKey());
       System.out.println(JSON.toJSONString(getActionInfo().getControllerMappingInfo()));
       System.out.println(JSON.toJSONString(getActionInfo().getControllerMethodInfo()));
       System.out.println(JSON.toJSONString(getActionInfo().getControllerInfo()));
        System.out.println("验证错误");
        controller.renderFreeMarker("test.html.tpl");
    }

    @Override
    protected void handleSuccess(Controller controller) {
        System.out.println("验证成功");
    }
}
