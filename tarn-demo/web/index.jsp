<%@ page import="cc.ruixing.framework.tarn.core.Tarn" %>
<%@ page import="cc.ruixing.framework.tarn.core.DAO" %>
<%@ page import="java.util.List" %>
<%--
  Created by IntelliJ IDEA.
  User: dfr
  Date: 14-10-14
  Time: 下午8:44
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title></title>
  </head>
  <body>
<%
    Tarn tarn=new Tarn();
    List<DAO> list=tarn.daos("SELECT * FROM sys_users");
    for(DAO c:list){
        out.print(c.get("name"));
    }
%>
  </body>
</html>
