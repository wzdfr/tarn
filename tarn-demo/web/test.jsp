<%--
  Created by IntelliJ IDEA.
  User: dfr
  Date: 15-5-4
  Time: 下午2:38
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
</head>
<body>
<form id="inputForm" action="hello/save/" method="post" enctype="multipart/form-data">
    <table width="100%" border="0" align="center" cellpadding="0"
           class="table_all_border" cellspacing="0" style="margin-bottom: 0px;border-bottom: 0px">
        <tr>
            <td class="td_table_top" align="center">
                流程部署
            </td>
        </tr>
    </table>
    <table class="table_all" align="center" border="0" cellpadding="0"
           cellspacing="0" style="margin-top: 0px">
        <tr>
            <td class="td_table_1">
                <span>上传流程定义文件：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="file" class="input_file" id="file" name="file"/>
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>名称：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="name" id="name">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>sex：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="sex" id="sex">
            </td>
        </tr>
    </table>
    <table align="center" border="0" cellpadding="0"
           cellspacing="0">
        <tr align="left">
            <td colspan="1">
                <input type="submit" class="button_70px" name="submit" value="提交">
                &nbsp;&nbsp;
                <input type="button" class="button_70px" name="reback" value="返回"
                       onclick="history.back()">
            </td>
        </tr>
    </table>

</form>
</body>
</html>
