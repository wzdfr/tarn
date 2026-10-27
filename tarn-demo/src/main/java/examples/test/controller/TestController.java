package examples.test.controller;

import cc.ruixing.framework.tarn.aop.annotation.*;
import cc.ruixing.framework.tarn.core.Config;
import cc.ruixing.framework.tarn.core.Controller;
import cc.ruixing.framework.tarn.upload.UploadFile;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import examples.test.emtity.Hello;
import examples.test.validate.HelloValidate;

/**
 * Created by dfr on 15-5-4.
 */
@EController("/ex/${mapping*}")
@ViewRootPath("/testa")
public class TestController extends Controller {

    private String name;

    private Hello hello;

    public void execute(){
        System.out.println("ABBBBBBBBBBBBBBB");
    }

    @ERequestMapping(value = "index", method = RequestMethod.GET)
    public String index() {
        return "test.jsp";
    }

    @ERequestMapping(value = "index2", method = RequestMethod.GET)
    public String index2() {
        return "test.html.tpl";
    }


    @ERequestMapping(value = "{*}.rx",isPathVariable = true)
    public String abadfd (@EPathVariable("nameKey")String namekey,@EPathVariable("INDEX_1")String path){
        System.out.println("namekey:"+namekey);
        System.out.println("path:"+path);
        System.out.println("BBBB");
        return "sss";
    }

    @ERequestMapping(value = "save", method = RequestMethod.POST)
    @EResponseType(false)
    public String save() {
        UploadFile uploadFile2 = getFile("file2", Config.getContextRealPath()+"/test/zzz/11/",1024*1024*10);
        String sex = get("sex");

        System.out.println(" name:"+name);
        System.out.println(" sex:"+sex);


        System.out.println(" file2:"+uploadFile2.getFileName());
        System.out.println(" file2 JSON:"+ JSON.toJSONString(uploadFile2));

        UploadFile uploadFile = getFile("file");
        System.out.println(" file:"+uploadFile.getFileName());
        System.out.println(" file JSON:"+ JSON.toJSONString(uploadFile));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 1);
        jsonObject.put("hello", hello);
        jsonObject.put("name", name);
        jsonObject.put("sex", sex);
        jsonObject.put("msg", "success");
        return jsonObject.toJSONString();
    }

    @ERequestMapping(value = "save2", method = RequestMethod.POST)
    @EResponseType(false)
    @Before(HelloValidate.class)
    public String save2() {
        System.out.println("");
        String sex = get("sex");
        System.out.println(" name:"+name);
        System.out.println(" sex:"+sex);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 1);
        jsonObject.put("hello", hello);
        jsonObject.put("name", name);
        jsonObject.put("sex", sex);
        jsonObject.put("msg", "success");
        return jsonObject.toJSONString();
    }

    @ERequestMapping(value = "delete/{id}", method = RequestMethod.POST, isPathVariable = true)
    @EResponseType(false)
    public String delete() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 1);
        jsonObject.put("msg", "success");
        return jsonObject.toJSONString();
    }

    @ERequestMapping(value = "direct", method = RequestMethod.POST, isPathVariable = true)
    public void dirrectHttp() {
        renderDirect("http://www.baidu.com");
    }

    @ERequestMapping(value = "direct2", method = RequestMethod.POST, isPathVariable = true)
    public void dirrect() {
        renderDirect("/server/hello");
    }


    public Hello getHello() {
        return hello;
    }

    public void setHello(Hello hello) {
        this.hello = hello;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
