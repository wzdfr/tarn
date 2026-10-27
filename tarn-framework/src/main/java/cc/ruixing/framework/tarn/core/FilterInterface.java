package cc.ruixing.framework.tarn.core;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 扩展Filter接口
 * */
public interface FilterInterface {

   /**
    * 初始化，在服务启动时值执行一次
    * */
    public void init(FilterConfig filterConfig);

    /**
     * @param request   ServletRequest
     * @param response   ServletResponse对象
     * @param filterChain  FilterChain对象
     *@return boolean 是否继续往下执行，返回true，是，false，不往下执行
     * */
    public boolean doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain);

    /**
     * 销毁
     * */
    public void destroy();

}
