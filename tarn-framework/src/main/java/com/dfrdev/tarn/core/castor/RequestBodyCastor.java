package com.dfrdev.tarn.core.castor;

import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;

public class RequestBodyCastor extends AbstractCastor {
    private static RequestBodyCastor singleton = new RequestBodyCastor();

    public static RequestBodyCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return HttpServletRequest.class == type;
    }

    public Object cast(Object obj, Class<?> type) throws  IOException {
        if (obj == null) {
            return null;
        }

        if (obj instanceof HttpServletRequest) {

            HttpServletRequest request = (HttpServletRequest) obj;
            String body = IOUtils.toString(request.getReader());
//            String body = readData(request.getReader());
            return body;
        }

        return null;
    }

    public static String readData(BufferedReader reader) {
        try {
            StringBuilder ret;
            String line = reader.readLine();
            if (line != null) {
                ret = new StringBuilder();
                ret.append(line);
            } else {
                return "";
            }
            while ((line = reader.readLine()) != null) {
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
}

