package examples.test.controller;

import cc.ruixing.framework.tarn.aop.annotation.*;
import cc.ruixing.framework.tarn.core.Controller;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

@EController("/api/v1/login/${mapping}")
public class TestAPI extends Controller {


    private String getRequestPayload(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = req.getReader();
            char[]buff = new char[1024];
            int len;
            while((len = reader.read(buff)) != -1) {
                sb.append(buff,0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    public  String readData(HttpServletRequest request) {
//        IOUtils.toString(request.getReader())

        BufferedReader br = null;
        try {
            StringBuilder ret;
            br = request.getReader();
            String line = br.readLine();
            if (line != null) {
                ret = new StringBuilder();
                ret.append(line);
            } else {
                return "";
            }

            while ((line = br.readLine()) != null) {
                ret.append('\n').append(line);
            }

            return ret.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		/* 去掉 close() 否则后续 ActionReporter 中的 getPara() 在部分 tomcat 中会报 IOException : Stream closed
		finally {
			if (br != null) {
				try {br.close();} catch (IOException e) {LogKit.error(e.getMessage(), e);}
			}
		}*/
    }

    @ERequestMapping(value = "login", method = RequestMethod.POST)
    @EResponseType(false)
    public String doLogin(@ERequestBody JSONObject body, @ERequestHeader("usertoken")String usertoken) throws SQLException {
//        String  body22=readData(getRequest());
        String  body2233=getRequestPayload(getRequest());
//        System.out.println("body22:"+body22);
        System.out.println("body2233:"+body2233);
        System.out.println("body:"+body);
        System.out.println("usertoken:"+usertoken);
        return "success";
    }
}
