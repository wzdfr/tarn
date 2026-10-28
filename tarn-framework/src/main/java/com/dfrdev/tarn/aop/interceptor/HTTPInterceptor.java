package com.dfrdev.tarn.aop.interceptor;

import com.dfrdev.tarn.aop.ActionInvocation;
import com.dfrdev.tarn.core.boot.Tarn;
import com.dfrdev.tarn.core.web.PageContext;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.TextKit;
import com.oreilly.servlet.MultipartRequest;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


public class HTTPInterceptor {

    private static HashMap<String, Object> mol = null;
    //private Map<String, Object> mol = new HashMap();

    /**
     * 完成类字段的setAttribute
     *
     * @param o       需要执行的Object对象
     * @param request 当前请求参数
     * @throws java.io.IOException,javax.servlet.ServletException
     */
    public static void setAttr(Object o, HttpServletRequest request) throws IOException, ServletException {
        Class clazz = o.getClass();
        Field fields[] = clazz.getDeclaredFields();
        for (Field field : fields) {
            Object value = null;
            String fieldName = field.getName();
            try {
                value = clazz.getMethod("get" + TextKit.firstLetterToUpper(fieldName)).invoke(o);
                if (value != null) request.setAttribute(fieldName, value);
            } catch (Exception e) {
                LogUtil.error("操作失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 完成set API和 PageContext
     */
    public static void setObj(Object o) {
        Class clazz = o.getClass();
        Field fields[] = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                String fieldName = field.getName();
                Class ft = field.getType();
                if (ft.equals(Tarn.class)) {
                    clazz.getMethod("set" + TextKit.firstLetterToUpper(fieldName), ft).invoke(o, new Tarn());
                }
                if (ft.equals(PageContext.class)) {
                    clazz.getMethod("set" + TextKit.firstLetterToUpper(fieldName), ft).invoke(o, Tarn.getPageContext());
                }
                //todo
            } catch (Exception e) {
                LogUtil.error("操作失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 完成属性注入
     *
     * @param request 当前请求对象。
     * @param o       需要执行注入的Action对象
     */
    public static void methodInj(HttpServletRequest request, Object o) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);//检查输入请求是否为multipart表单数据。
        Class clazz = o.getClass();
//        if (isMultipart == true) {
//            ergodicMultipartFile(request, o);
//        } else {
        Enumeration paramNames = request.getParameterNames();
        mol = new HashMap<String, Object>();
        Object object = null;
        while (paramNames.hasMoreElements()) {

            String pn = (String) paramNames.nextElement();
            String sval = request.getParameter(pn);
            if (pn.indexOf(".") != -1) {
                String[] temp = pn.split("\\.");
                String cn = temp[0];
                String cp = temp[1];
                Field field = null;
                try {
                    field = clazz.getDeclaredField(cn);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                Object tmp = mol.get(cn);
                if (tmp == null) {
                    object = field.getType().newInstance();
                }
                if (!sval.equals("") || sval != null)
                    ActionInvocation.invokeSet(tmp == null ? object : mol.get(cn), cp, sval);
                if (mol.get(cn) == null || mol == null) {
                    // object = Class.forName(TextKit.firstLetterToUpper(cn)).newInstance();
                    if (object != null) {
                        mol.put(cn, object);
                    }
                }
            } else {
                Field field = null;
                try {
                    field = clazz.getDeclaredField(pn);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                if (!sval.equals("") || sval != null) ActionInvocation.invokeSet(o, pn, sval);
            }
        }
        if (mol.size() > 0 || mol != null) {
            for (Map.Entry<String, Object> m : mol.entrySet()) {
                clazz.getMethod("set" + TextKit.firstLetterToUpper(m.getKey()), new Class[]{clazz.getDeclaredField(m.getKey()).getType()}).invoke(o, new Object[]{m.getValue()});
            }
        }
        //  }

    }

    public static void ergodicMultipartFile(HttpServletRequest request, Object o) throws IOException, IllegalAccessException, InstantiationException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException {
        MultipartRequest mreq = new MultipartRequest(request, ".");

        Enumeration paramNames = mreq.getParameterNames();
        Class clazz = o.getClass();
        mol = new HashMap<String, Object>();
        Object object = null;
        while (paramNames.hasMoreElements()) {

            String pn = (String) paramNames.nextElement();
            String sval = mreq.getParameter(pn);
            if (pn.indexOf(".") != -1) {
                String[] temp = pn.split("\\.");
                String cn = temp[0];
                String cp = temp[1];
                Field field = null;
                try {
                    field = clazz.getDeclaredField(cn);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                Object tmp = mol.get(cn);
                if (tmp == null) {
                    object = field.getType().newInstance();
                }
                if (!sval.equals("") || sval != null)
                    ActionInvocation.invokeSet(tmp == null ? object : mol.get(cn), cp, sval);
                if (mol.get(cn) == null || mol == null) {
                    // object = Class.forName(TextKit.firstLetterToUpper(cn)).newInstance();
                    if (object != null) {
                        mol.put(cn, object);
                    }
                }
            } else {
                Field field = null;
                try {
                    field = clazz.getDeclaredField(pn);
                } catch (NoSuchFieldException e) {
                    continue;
                }
                if (!sval.equals("") || sval != null) ActionInvocation.invokeSet(o, pn, sval);
            }
        }
        if (mol.size() > 0 || mol != null) {
            for (Map.Entry<String, Object> m : mol.entrySet()) {
                clazz.getMethod("set" + TextKit.firstLetterToUpper(m.getKey()), new Class[]{clazz.getDeclaredField(m.getKey()).getType()}).invoke(o, new Object[]{m.getValue()});
            }
        }

        //todo
        if (Tarn.isDebug()) LogUtil.debug("带有上传文件的请求");
    }

}
