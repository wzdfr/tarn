package cc.ruixing.framework.tarn.core;


import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


public class DefaultFilterImpl  implements FilterInterface {

    @Override
    public void init(FilterConfig filterConfig) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
        //To change body of implemented methods use File | Settings | File Templates.
        return true;
    }

    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
