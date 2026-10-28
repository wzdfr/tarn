package com.dfrdev.tarn.aop.interceptor;

import com.dfrdev.tarn.aop.ControllerMappingInfo;
import com.dfrdev.tarn.aop.ControllerMethodInfo;
import com.dfrdev.tarn.aop.annotation.ERequestMapping;
import com.dfrdev.tarn.aop.annotation.RequestMethod;
import com.dfrdev.tarn.aop.ControllerInfo;
import com.dfrdev.tarn.plugin.ehcache.TarnCacheKit;
import com.dfrdev.tarn.util.classloader.ClassUtil;
import com.dfrdev.tarn.util.LogUtil;
import com.dfrdev.tarn.util.text.StringUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dfr on 14-1-10.
 */
public class RequestMappingUtils {
    /**
     * 检测${mapping}和 ${method}
     */

    public static ControllerMappingInfo installMethod(ControllerInfo pi, String vpath, String methodType) {
        String k = pi.getKey();
        String monthod = "";
        String mappingName = "";
        String path = "";
        /***
         * 如果拦截器配置是${mapping}
         * */
        if (k.indexOf("${mapping}") != -1) {
            String regex = StringUtil.isEmpty(pi.getRegex()) ? "[a-zA-Z0-9_]+" : pi.getRegex();
            String reg = k.replace("${mapping}", regex);
            Matcher matcher = Pattern.compile(reg).matcher(vpath);
           if(StringUtil.isNotEmpty(pi.getRegex())) LogUtil.debug("controller path " + regex + " " + vpath);
            if (matcher.find()) {
                path = vpath.replace(k.substring(0, k.indexOf("$")), "").replace(k.substring(k.indexOf("g}") + 2), "");
                List<ControllerMethodInfo> list = pi.getMethodInfoList();
//                List<ControllerMapping> list = (List<ControllerMapping>) TarnCacheKit.get("Tarn.RequestMapping." + pi.getValue());
                if (list == null || list.size() == 0) {
                    List requestMappingItem = new ArrayList();
                    try {
                        Object o = Class.forName(pi.getValue()).newInstance();
                        Method method[] = o.getClass().getDeclaredMethods();
                        for (Method m : method) {
                            if (m.isAnnotationPresent(ERequestMapping.class)) {
                                ERequestMapping requestMapping = m.getAnnotation(ERequestMapping.class);
                                RequestMethod rm = requestMapping.method();
                                ControllerMethodInfo controllerMethodInfo = new ControllerMethodInfo();
                                controllerMethodInfo.setName(m.getName());
                                controllerMethodInfo.setReturnType(m.getReturnType());
                                controllerMethodInfo.setParameters(m.getParameterTypes().length > 0);
                                controllerMethodInfo.setParamTypes(m.getParameterTypes());
                                controllerMethodInfo.setRequestMapping(requestMapping);
                                mappingName = requestMapping.value();
                                requestMappingItem.add(controllerMethodInfo);
                                if (requestMapping.isPathVariable()) {
                                    String reg2 = "";
                                    if (StringUtil.isNotEmpty(requestMapping.regex())) {
                                        reg2 = "^" + requestMapping.value().replaceAll("\\{.*?\\}", requestMapping.regex()) + "$";
                                    } else {
                                        reg2 = "^" + requestMapping.value().replaceAll("\\{.*?\\}", "([a-zA-Z0-9_]+)") + "$";
                                        if (requestMapping.value().indexOf("{*}") != -1) {
                                            reg2 = "^" + requestMapping.value().replaceAll("\\{.*?\\}", "([a-zA-Z0-9_/|~!@]+)") + "$";
                                        }
                                    }

//                                    LogUtil.debug("regex:" + reg2);
//                                    LogUtil.debug("path:" + path);
                                    Matcher matcher2 = Pattern.compile(reg2).matcher(path);
                                    if (matcher2.find()) {
                                        if (rm != RequestMethod.AUTO) {
                                            if (methodType.equals(rm.toString())) {
                                                monthod = m.getName();
                                            }
                                        } else {
                                            monthod = m.getName();
                                        }
                                    }
                                } else if (requestMapping.value().equals(path)) {
                                    if (rm != RequestMethod.AUTO) {
                                        if (methodType.equals(rm.toString())) {
                                            monthod = m.getName();
                                        }
                                    } else {
                                        monthod = m.getName();
                                    }
                                }

                            }
                        }
                        pi.setMethodInfoList(requestMappingItem);
//                        TarnCacheKit.put("Tarn.RequestMapping." + pi.getValue(), requestMappingItem);
                    } catch (InstantiationException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    } catch (IllegalAccessException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    } catch (ClassNotFoundException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    }
                } else {
                    for (ControllerMethodInfo m : list) {
                        mappingName = m.getRequestMapping().value();
                        if (m.getRequestMapping().isPathVariable()) {
                            String reg2 = "";
                            if (StringUtil.isNotEmpty(m.getRequestMapping().regex())) {
                                reg2 = "^" + m.getRequestMapping().value().replaceAll("\\{.*?\\}", m.getRequestMapping().regex()) + "$";
                            } else {
                                reg2 = "^" + mappingName.replaceAll("\\{.*?\\}", "([a-zA-Z0-9_]+)") + "$";
                                if (mappingName.indexOf("{*}") != -1) {
                                    reg2 = "^" + mappingName.replaceAll("\\{.*?\\}", "([a-zA-Z0-9_/|]+)") + "$";
                                }
                            }
//                            LogUtil.debug("regex:" + reg2);
//                            LogUtil.debug("path:" + path);
                            Matcher matcher2 = Pattern.compile(reg2).matcher(path);
                            if (matcher2.find()) {
                                if (m.getRequestMapping().method() != RequestMethod.AUTO) {
                                    if (m.getRequestMapping().method().toString().equals(methodType)) {
                                        monthod = m.getName();
                                        break;
                                    }
                                } else {
                                    monthod = m.getName();
                                    break;
                                }
                            }
                        } else if (mappingName.equals(path)) {
                            if (m.getRequestMapping().method() != RequestMethod.AUTO) {
                                if (m.getRequestMapping().method().toString().equals(methodType)) {
                                    monthod = m.getName();
                                    break;
                                }
                            } else {
                                monthod = m.getName();
                                break;
                            }

                        }
                    }
                }
            }
        }
        if (k.indexOf("${mapping*}") != -1) {
            String reg = k.replace("${mapping*}", "[a-zA-Z0-9_/|]+");
            Matcher matcher = Pattern.compile(reg).matcher(vpath);
            if (matcher.find()) {
                path = vpath.replace(k.substring(0, k.indexOf("$")), "").replace(k.substring(k.indexOf("g*}") + 3), "");
                List<ControllerMethodInfo> list = pi.getMethodInfoList();
//                List<ControllerMapping> list = (List<ControllerMapping>) TarnCacheKit.get("Tarn.RequestMapping." + pi.getValue());
                if (list == null || list.size() == 0) {
                    List requestMappingItem = new ArrayList();
                    try {
                        Object o = Class.forName(pi.getValue()).newInstance();
                        Method method[] = o.getClass().getDeclaredMethods();
                        for (Method m : method) {
                            if (m.isAnnotationPresent(ERequestMapping.class)) {
                                ERequestMapping requestMapping = m.getAnnotation(ERequestMapping.class);
                                RequestMethod rm = requestMapping.method();
                                ControllerMethodInfo controllerMethodInfo = new ControllerMethodInfo();
                                controllerMethodInfo.setName(m.getName());
                                controllerMethodInfo.setReturnType(m.getReturnType());
                                controllerMethodInfo.setParameters(m.getParameterTypes().length > 0);
                                controllerMethodInfo.setParamTypes(m.getParameterTypes());
                                controllerMethodInfo.setRequestMapping(requestMapping);
                                mappingName = requestMapping.value();
                                requestMappingItem.add(controllerMethodInfo);
                                if (requestMapping.isPathVariable()) {
                                    String reg2 = "^" + requestMapping.value().replaceAll("\\{.*?\\}", "([a-zA-Z0-9_]+)") + "$";
                                    if (requestMapping.value().indexOf("{*}") != -1) {
                                        reg2 = "^" + requestMapping.value().replaceAll("\\{.*?\\}", "([a-zA-Z0-9_/|]+)") + "$";
                                    }
                                    LogUtil.debug("reg2:" + reg2);
                                    LogUtil.debug("path:" + path);
                                    Matcher matcher2 = Pattern.compile(reg2).matcher(path);
                                    if (matcher2.find()) {
                                        if (rm != RequestMethod.AUTO) {
                                            if (methodType.equals(rm.toString())) {
                                                monthod = m.getName();
                                            }
                                        } else {
                                            monthod = m.getName();
                                        }
                                    }
                                } else if (requestMapping.value().equals(path)) {
                                    if (rm != RequestMethod.AUTO) {
                                        if (methodType.equals(rm.toString())) {
                                            monthod = m.getName();
                                        }
                                    } else {
                                        monthod = m.getName();
                                    }
                                }

                            }
                        }
                        pi.setMethodInfoList(requestMappingItem);
//                        TarnCacheKit.put("Tarn.RequestMapping." + pi.getValue(), requestMappingItem);
                    } catch (InstantiationException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    } catch (IllegalAccessException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    } catch (ClassNotFoundException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    }
                } else {
                    for (ControllerMethodInfo m : list) {
                        mappingName = m.getRequestMapping().value();
                        if (m.getRequestMapping().isPathVariable()) {
                            String reg2 = "^" + mappingName.replaceAll("\\{.*?\\}", "([a-zA-Z0-9_]+)") + "$";
                            if (mappingName.indexOf("{*}") != -1) {
                                reg2 = "^" + mappingName.replaceAll("\\{.*?\\}", "([a-zA-Z0-9_/|]+)") + "$";
                            }
                            Matcher matcher2 = Pattern.compile(reg2).matcher(path);
                            if (matcher2.find()) {
                                if (m.getRequestMapping().method() != RequestMethod.AUTO) {
                                    if (m.getRequestMapping().method().toString().equals(methodType)) {
                                        monthod = m.getName();
                                        break;
                                    }
                                } else {
                                    monthod = m.getName();
                                    break;
                                }
                            }
                        } else if (mappingName.equals(path)) {
                            if (m.getRequestMapping().method() != RequestMethod.AUTO) {
                                if (m.getRequestMapping().method().toString().equals(methodType)) {
                                    monthod = m.getName();
                                    break;
                                }
                            } else {
                                monthod = m.getName();
                                break;
                            }

                        }
                    }
                }
            }
        }

        /**
         * 如果拦截器配置${method}
         * */
        if (k.indexOf("${method}") != -1) {
            String reg = k.replace("${method}", "[a-zA-Z0-9_]+");
            Matcher matcher = Pattern.compile(reg).matcher(vpath);
            if (matcher.find()) {
                monthod = vpath.replace(k.substring(0, k.indexOf("$")), "")
                        .replace(k.substring(k.indexOf("d}") + 2), "");
            }
        }

        ControllerMappingInfo controllerMapping = new ControllerMappingInfo();
        controllerMapping.setRequestMapping(k);
        controllerMapping.setMethodName(monthod);
        controllerMapping.setMappingName(mappingName);
        controllerMapping.setPath(path);
        return controllerMapping;

    }

    public static void addMethodItem(String classPath, String mappingName, String methodType) {
        List requestMappingItem = new ArrayList();
        String monthod = "";
        try {
            Object o = Class.forName(classPath).newInstance();
            Method method[] = o.getClass().getDeclaredMethods();
            for (Method m : method) {
                if (m.isAnnotationPresent(ERequestMapping.class)) {
                    ERequestMapping requestMapping = m.getAnnotation(ERequestMapping.class);
                    RequestMethod rm = requestMapping.method();
                    requestMappingItem.add(new ControllerMappingInfo(requestMapping.value(), m.getName(), rm, requestMapping.isPathVariable()));
                    if (requestMapping.value().equals(mappingName)) {
                        if (rm != RequestMethod.AUTO) {
                            if (methodType.equals(rm.toString())) {
                                monthod = m.getName();
                            }
                        } else {
                            monthod = m.getName();
                        }
                    }
                }
            }
            TarnCacheKit.put("Tarn.RequestMapping." + classPath, requestMappingItem);
        } catch (InstantiationException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            LogUtil.error("操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检测${mapping}
     */

    public static ControllerMappingInfo installMethodMapping(ControllerInfo pi, String vpath, String methodType) {
        String k = pi.getKey();
        String monthod = "";
        String path = "";
        String mappingName = "";
        boolean isreg = false;
        /***
         * 如果拦截器配置是${mapping}
         * */
        if (k.indexOf("${mapping}") != -1) {
            String reg = k.replace("${mapping}", "[a-zA-Z0-9_]+");
            Matcher matcher = Pattern.compile(reg).matcher(vpath);
            if (matcher.find()) {
                path = vpath.replace(k.substring(0, k.indexOf("$")), "").replace(k.substring(k.indexOf("g}") + 2), "");
                List<ControllerMethodInfo> list = pi.getMethodInfoList();
//                List<ControllerMapping> list = (List<ControllerMapping>) TarnCacheKit.get("Tarn.RequestMapping." + pi.getValue());
                if (list == null || list.size() == 0) {
                    List requestMappingItem = new ArrayList();
                    try {
                        Object o = Class.forName(pi.getValue()).newInstance();
                        Method method[] = o.getClass().getDeclaredMethods();
                        for (Method m : method) {
                            if (m.isAnnotationPresent(ERequestMapping.class)) {
                                ERequestMapping requestMapping = m.getAnnotation(ERequestMapping.class);
                                RequestMethod rm = requestMapping.method();
                                ControllerMethodInfo controllerMethodInfo = new ControllerMethodInfo();
                                controllerMethodInfo.setName(m.getName());
                                controllerMethodInfo.setReturnType(m.getReturnType());
                                controllerMethodInfo.setParameters(m.getParameterTypes().length > 0);
                                controllerMethodInfo.setParamTypes(m.getParameterTypes());
                                controllerMethodInfo.setRequestMapping(requestMapping);
                                mappingName = requestMapping.value();
                                requestMappingItem.add(controllerMethodInfo);
                                if (requestMapping.isPathVariable()) {
                                    String reg2 = "^" + requestMapping.value().replaceAll("\\{.*?\\}", "([a-zA-Z0-9_]+)") + "$";
                                    LogUtil.debug("reg2:" + reg2);
                                    LogUtil.debug("path:" + path);
                                    Matcher matcher2 = Pattern.compile(reg2).matcher(path);
                                    if (matcher2.find()) {
                                        if (rm != RequestMethod.AUTO) {
                                            if (methodType.equals(rm.toString())) {
                                                monthod = m.getName();
                                            }
                                        } else {
                                            monthod = m.getName();
                                        }
                                    }
                                } else if (requestMapping.value().equals(path)) {
                                    if (rm != RequestMethod.AUTO) {
                                        if (methodType.equals(rm.toString())) {
                                            monthod = m.getName();
                                        }
                                    } else {
                                        monthod = m.getName();
                                    }
                                }

                            }
                        }
                        pi.setMethodInfoList(requestMappingItem);
//                        TarnCacheKit.put("Tarn.RequestMapping." + pi.getValue(), requestMappingItem);
                    } catch (InstantiationException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    } catch (IllegalAccessException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    } catch (ClassNotFoundException e) {
                        LogUtil.error("操作失败: " + e.getMessage(), e);
                    }
                } else {
                    for (ControllerMethodInfo m : list) {
                        mappingName = m.getRequestMapping().value();
                        if (m.getRequestMapping().isPathVariable()) {
                            String reg2 = "^" + mappingName.replaceAll("\\{.*?\\}", "([a-zA-Z0-9_]+)") + "$";
                            Matcher matcher2 = Pattern.compile(reg2).matcher(path);
                            if (matcher2.find()) {
                                if (m.getRequestMapping().method() != RequestMethod.AUTO) {
                                    if (m.getRequestMapping().method().toString().equals(methodType)) {
                                        monthod = m.getName();
                                        break;
                                    }
                                } else {
                                    monthod = m.getName();
                                    break;
                                }
                            }
                        } else if (mappingName.equals(path)) {
                            if (m.getRequestMapping().method() != RequestMethod.AUTO) {
                                if (m.getRequestMapping().method().toString().equals(methodType)) {
                                    monthod = m.getName();
                                    break;
                                }
                            } else {
                                monthod = m.getName();
                                break;
                            }

                        }
                    }
                }
            }
        }

        ControllerMappingInfo controllerMapping = new ControllerMappingInfo();
        controllerMapping.setMethodName(monthod);
        controllerMapping.setPath(path);
        controllerMapping.setMappingName(mappingName);
//        controllerMapping.setViewType(rm);
        return controllerMapping;

    }


}
