package com.dfrdev.tarn.util;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class VisitIpAddr {
    /**
     * 获取客户端IP地址
     */


    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if( ip.indexOf(",")!=-1 ){
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if(StringUtil.isNotEmpty(ip)){
            if( ip.indexOf(",")!=-1 ){
                ip = ip.split(",")[0];
            }
        }
        return ip;
    }

    /**
     * 根据IP地址获取地理位置，如：北京市
     */
    public static String getAddressByIP(String ip){
        String ipAddress = ip;
        try{
            URL url = new URL("http://www.ip138.com/ips138.asp?ip=" + ip);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "GBK"));
            String line = null;
            StringBuffer result = new StringBuffer();
            while((line = reader.readLine()) != null){
                result.append(line);
            }
            reader.close();
            ip = result.substring(result.indexOf("主数据：") + 4, result.indexOf("</li><li>参考数据"));
            char[] ipStr = ip.toCharArray();
            for(int i = 0; i < ipStr.length; i++){
                if(i != 0 && !Character.isLetter(ipStr[i])){    //判断是否汉字，不含中文特殊符号
                    ip = ip.substring(0, i);
                    break;
                }
            }
        }catch(StringIndexOutOfBoundsException indexOut){
			      System.out.println("IP地址获取失败：下标越界\t" + ipAddress);
            return ipAddress;
        }catch(IOException e){
            return ipAddress;
        }
        return ip;
    }
}
