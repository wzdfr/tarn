package com.dfrdev.tarn.aop.service;

import com.dfrdev.tarn.aop.ControllerInfo;
import com.dfrdev.tarn.aop.ControllerMethodInfo;
import com.dfrdev.tarn.aop.Inject;
import com.dfrdev.tarn.aop.MethodParam;
import com.dfrdev.tarn.aop.annotation.EController;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.EResponseType;
import com.dfrdev.tarn.aop.annotation.ViewRootPath;
import com.dfrdev.tarn.collection.Mapx;
import com.dfrdev.tarn.util.classloader.ClassUtil;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.util.xml.XMLElement;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by dfr on 15-3-1.
 */
public class ControllerAOP implements LoadAOP {
    /**
     * 从目录加载映射
     *
     * @param dirs
     */
    @Override
    public boolean load(File dirs, XMLElement xmlElement, XMLElement writerXml) {
        ArrayList<String> list = new ArrayList<String>();
        for (File c : dirs.listFiles()) {
            String fn = c.getName();
            list.add(fn);
        }
        return process(list, xmlElement, writerXml, false);
    }

    /**
     * 从指定类集合加载映射
     *
     * @param packageAndClassList
     */
    @Override
    public boolean load(List<String> packageAndClassList, XMLElement xmlElement, XMLElement writerXml) {
        return process(packageAndClassList, xmlElement, writerXml, true);
    }

    @Override
    public boolean load(Set<Class<?>> packageAndClassList, XMLElement xmlElement, XMLElement writerXml) {
        Mapx mapx = xmlElement.getAttributes();
        XMLElement xml = null;
        for (Class<?> o : packageAndClassList) {
            if (o.getName().indexOf("$") == -1) {
                boolean aann = o.isAnnotationPresent(EController.class);
                if (aann) {
                    EController ann = (EController) o.getAnnotation(EController.class);
                    ControllerInfo pi = new ControllerInfo(ann.value(), o.getName());
                    Method methods[] = null;
                    if (ann.selfMethods()) {
                        methods = o.getDeclaredMethods();
                    } else {
                        methods = o.getMethods();
                    }
                    if(StringUtil.isNotEmpty(ann.regex())) pi.setRegex(ann.regex());
                    List methodList = new ArrayList();
                    XMLElement xmlcontroller = writerXml.addElement("controller");
                    xmlcontroller.addAttribute("class", o.getName());
                    xmlcontroller.addAttribute("rule", ann.value());
                    xmlcontroller.addAttribute("time", new Date().toString());
                    if (o.isAnnotationPresent(ViewRootPath.class)) {
                        ViewRootPath viewRootPath = (ViewRootPath) o.getAnnotation(ViewRootPath.class);
                        if (StringUtil.isNotEmpty(viewRootPath.value())) {
                            pi.setViewRootPath(viewRootPath.value());
                            xmlcontroller.addAttribute("viewRootPath", viewRootPath.value());
                        }

                    }

                    boolean isMethod = pi.getKey().indexOf("${method}") == -1;
                    for (Method m : methods) {
                        String mstr[] = m.toString().split(" ");
                        boolean next = false;
                        if (mstr.length > 0) {
                            if (mstr[0].equalsIgnoreCase("public") || mstr[0].equalsIgnoreCase("protected")) {
                                next = true;
                            }
                        } else next = true;
                        if (next) {
                            if (!m.getName().startsWith("get") && !m.getName().startsWith("set")) {
                                if (m.isAnnotationPresent(ERequestMapping.class) || m.isAnnotationPresent(EResponseType.class) || !isMethod) {
                                    ControllerMethodInfo controllerMethodInfo = new ControllerMethodInfo();
                                    controllerMethodInfo.setName(m.getName());
                                    controllerMethodInfo.setReturnType(m.getReturnType());
                                    controllerMethodInfo.setParameters(m.getParameterTypes().length > 0);
                                    controllerMethodInfo.setParamTypes(m.getParameterTypes());
                                    if (m.getParameterTypes().length > 0) {
                                        Annotation annotations[][] = m.getParameterAnnotations();
                                        int ii = 0;
                                        Object obj[] = new Object[m.getParameterTypes().length];
                                        for (Annotation[] annp : annotations) {
                                            MethodParam methodParam = null;
                                            for (Annotation an : annp) {
                                                methodParam = controllerMethodInfo.bulidMethodPram(an);
                                            }
                                            methodParam.setIndex(ii);
                                            obj[ii] = methodParam;
                                            controllerMethodInfo.setAnnotations(annp);
                                            ii++;
                                        }
                                        controllerMethodInfo.setMethodParam(obj);
                                    }
                                    XMLElement xmlMethod = xmlcontroller.addElement("method");
                                    xmlMethod.addAttribute("name", controllerMethodInfo.getName());
                                    xmlMethod.addAttribute("isParameters", controllerMethodInfo.isParameters() + "");
                                    if (isMethod) {
                                        ERequestMapping eRequestMapping = m.getAnnotation(ERequestMapping.class);
                                        if (eRequestMapping != null) {
                                            controllerMethodInfo.setRequestMapping(eRequestMapping);
                                            xmlMethod.addAttribute("mappingName", eRequestMapping.value());
                                            xmlMethod.addAttribute("requestMethod", eRequestMapping.method().name());
                                            xmlMethod.addAttribute("isPathVariable", eRequestMapping.isPathVariable() + "");
                                        }
                                    }
                                    methodList.add(controllerMethodInfo);
                                }
                            }
                        }
                    }

                    pi.setMethodInfoList(methodList);
                    Inject.setController(pi);
                }

                LogUtil.info("Controller Scanning success >> " + ClassUtil.getShortClassName(o.getName()));
            }

        }
        return true;
    }

    private boolean process(List<String> packageAndClassList, XMLElement xmlElement, XMLElement writerXml, boolean isCompletePackageName) {
        Mapx mapx = xmlElement.getAttributes();
        XMLElement xml = null;
        for (String classAllName : packageAndClassList) {
            boolean status = isCompletePackageName ? true : (classAllName.indexOf(".class") != -1);
            if (status && classAllName.indexOf("$") == -1) {
                String pcn = (isCompletePackageName) ? classAllName : mapx.getString("package") + "." + classAllName.substring(0, classAllName.lastIndexOf("."));
                Class o = null;
                try {
                    o = Class.forName(pcn);
                    boolean aann = o.isAnnotationPresent(EController.class);
                    if (aann) {
                        EController ann = (EController) o.getAnnotation(EController.class);
                        ControllerInfo pi = new ControllerInfo(ann.value(), pcn);
                        Method methods[] = null;
                        if (ann.selfMethods()) {
                            methods = o.getDeclaredMethods();
                        } else {
                            methods = o.getMethods();
                        }
                        List methodList = new ArrayList();
                        XMLElement xmlcontroller = writerXml.addElement("controller");
                        xmlcontroller.addAttribute("class", pcn);
                        xmlcontroller.addAttribute("rule", ann.value());
                        xmlcontroller.addAttribute("time", new Date().toString());
                        if (o.isAnnotationPresent(ViewRootPath.class)) {
                            ViewRootPath viewRootPath = (ViewRootPath) o.getAnnotation(ViewRootPath.class);
                            if (StringUtil.isNotEmpty(viewRootPath.value())) {
                                pi.setViewRootPath(viewRootPath.value());
                                xmlcontroller.addAttribute("viewRootPath", viewRootPath.value());
                            }

                        }

                        boolean isMethod = pi.getKey().indexOf("${method}") == -1;
                        for (Method m : methods) {
                            String mstr[] = m.toString().split(" ");
                            boolean next = false;
                            if (mstr.length > 0) {
                                if (mstr[0].equalsIgnoreCase("public") || mstr[0].equalsIgnoreCase("protected")) {
                                    next = true;
                                }
                            } else next = true;
                            if (next) {
                                if (!m.getName().startsWith("get") && !m.getName().startsWith("set")) {
                                    if (m.isAnnotationPresent(ERequestMapping.class) || m.isAnnotationPresent(EResponseType.class) || !isMethod) {
                                        ControllerMethodInfo controllerMethodInfo = new ControllerMethodInfo();
                                        controllerMethodInfo.setName(m.getName());
                                        controllerMethodInfo.setReturnType(m.getReturnType());
                                        controllerMethodInfo.setParameters(m.getParameterTypes().length > 0);
                                        controllerMethodInfo.setParamTypes(m.getParameterTypes());
                                        if (m.getParameterTypes().length > 0) {
                                            Annotation annotations[][] = m.getParameterAnnotations();
                                            int ii = 0;
                                            Object obj[] = new Object[m.getParameterTypes().length];
                                            for (Annotation[] annp : annotations) {
                                                MethodParam methodParam = null;
                                                for (Annotation an : annp) {
                                                    methodParam = controllerMethodInfo.bulidMethodPram(an);
                                                }
                                                methodParam.setIndex(ii);
                                                obj[ii] = methodParam;
                                                controllerMethodInfo.setAnnotations(annp);
                                                ii++;
                                            }
                                            controllerMethodInfo.setMethodParam(obj);
                                        }
                                        XMLElement xmlMethod = xmlcontroller.addElement("method");
                                        xmlMethod.addAttribute("name", controllerMethodInfo.getName());
                                        xmlMethod.addAttribute("isParameters", controllerMethodInfo.isParameters() + "");
                                        if (isMethod) {
                                            ERequestMapping eRequestMapping = m.getAnnotation(ERequestMapping.class);
                                            if (eRequestMapping != null) {
                                                controllerMethodInfo.setRequestMapping(eRequestMapping);
                                                xmlMethod.addAttribute("mappingName", eRequestMapping.value());
                                                xmlMethod.addAttribute("requestMethod", eRequestMapping.method().name());
                                                xmlMethod.addAttribute("isPathVariable", eRequestMapping.isPathVariable() + "");
                                            }
                                        }
                                        methodList.add(controllerMethodInfo);
                                    }
                                }
                            }
                        }

                        pi.setMethodInfoList(methodList);
                        Inject.setController(pi);
                    }

                } catch (ClassNotFoundException e) {
                    LogUtil.error("扫描 Controller 类失败: " + pcn + " -> " + e.getMessage(), e);
                }
                LogUtil.info("Controller Scanning success >> " + pcn);
            }

        }
        return true;
    }
}
