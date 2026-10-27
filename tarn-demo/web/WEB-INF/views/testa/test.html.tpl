<html>
<head>
    <title></title>
    <script src="http://libs.baidu.com/jquery/2.0.0/jquery.min.js"></script>

</head>
<body>
<#if INVALID_ERROR_STATUS>
    <div style="color: red">
        <h1>错误提示：</h1>
        <p>${nameMsg}</p>
        <p>${idMsg}</p>
        <p>${orgNameMsg}</p>
    </div>
</#if>
<form id="inputForm" action="save2" method="post" enctype="" >
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
                <span>上传流程定义文件：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="file" class="input_file" id="file2" name="file2"/>
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>名称：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="name" id="name" value="${name}">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>sex：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="sex" id="sex" value="${sex}">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>hello.name：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="hello.name" id="hename" value="${hello.name}">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>hello.org.id：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="hello.org.id" id="hello.org.id" value="${hello.org.id}">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>hello.org.name：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="hello.org.name" id="hello.org.name" value="${hello.org.name}">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>hello.org.role.id：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="hello.org.role.id" id="hello.org.role.id"  value="${hello.org.role.id}">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>hello.org.role.name：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="hello.org.role.name" id="hello.org.role.name"   value="${hello.org.role.name}">
            </td>
        </tr>
        <tr>
            <td class="td_table_1">
                <span>hello.id：</span>
            </td>
            <td class="td_table_2" colspan="3">
                <input type="text" name="hello.id" id="hello.id" value="${hello.id}">
            </td>
        </tr>

    </table>
    <button id="test" type="button">TEST</button>
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
<script src="https://unpkg.com/axios/dist/axios.min.js"></script>


<script>
    $(function(){
        $("#test").click(function(){

         return    axios({
                method: 'post',
             url: '/api/v1/login/login',
                data: {
                    firstName: 'Fred',
                    lastName: 'Flintstone'
                }
            });

            $.ajax({
                url: '/api/v1/login/login',
                type: 'POST',
                contentType: 'application/json; charset=utf-8', // 很重要
                traditional: true,
                data: "{\"username\":\"admin\",\"password\":\"111111\"}",
                success: function(res, status, xhr) {

                }
            });
        });
    })

</script>
</body>
</html>
