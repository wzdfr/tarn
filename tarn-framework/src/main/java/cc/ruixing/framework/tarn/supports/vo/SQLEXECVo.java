package cc.ruixing.framework.tarn.supports.vo;



import cc.ruixing.framework.tarn.aop.annotation.Ekey;
import cc.ruixing.framework.tarn.aop.annotation.Etable;

import javax.servlet.http.HttpServletRequest;

@Etable("efs_SqlExecuteLogs")
public class SQLEXECVo {
    @Ekey(key = "id",isAutoIncrement = true)
    private Integer id;
    private String precSql;//预编译SQL
    private String realSql;  //替换值后的SQL
    private String ip;
    private String addr;
    private String time;
    private HttpServletRequest request;
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPrecSql() {
        return precSql;
    }

    public void setPrecSql(String precSql) {
        this.precSql = precSql;
    }

    public String getRealSql() {
        return realSql;
    }

    public void setRealSql(String realSql) {
        this.realSql = realSql;
    }

    public SQLEXECVo(String precSql, String realSql, String ip, String addr, HttpServletRequest request) {
        this.addr = addr;
        this.ip = ip;
        this.precSql = precSql;
        this.realSql = realSql;
        this.request = request;
    }
    public SQLEXECVo(String precSql, String realSql, HttpServletRequest request) {
        this.precSql = precSql;
        this.realSql = realSql;
        this.request = request;
    }

    public SQLEXECVo(String precSql, String realSql) {
        this.precSql = precSql;
        this.realSql = realSql;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
