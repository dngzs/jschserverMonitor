<%@ page language="java" contentType="text/html; charset=utf-8"
pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<head>

    <meta charset="utf-8">
    <title>tail log</title>
    <script src="//cdn.bootcss.com/jquery/2.1.4/jquery.js"></script>
    <script src="${pageContext.request.contextPath}/static/plugs/bootstrap/js/bootstrap.min.js"></script>
    <link href="${pageContext.request.contextPath}/static/plugs/bootstrap/css/bootstrap.min.css" rel="stylesheet" />
    <link href="https://cdn.bootcss.com/font-awesome/4.7.0/css/font-awesome.min.css" rel="stylesheet" />

<style type="text/css">
    th,td{
        text-align:center;/** 设置水平方向居中 */
        vertical-align:middle;/** 设置垂直方向居中 */

        font-size: 18px;
    }
    td{
        color: #00a157;
    }
</style>
</head>
<body>
<table class="table table-striped">
    <table class="table">
        <caption style="background-color:#FF6100;font-size: 21px;color: #FFFFFF">${projectName}项目日志打印以及服务器状况</caption>
        <thead>
        <tr>
            <th>project</th>
            <th>进程</th>
            <th>ip</th>
            <th >cpu</th>
            <th >memory</th>
            <th >disk</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td >${projectName}</td>
            <td width="80px" id="status"></td>
            <td >${ip}</td>
            <td id="cpuinfo"></td>
            <td id="memoryinfo"></td>
            <td id="deskinfo"></td>
        </tr>
    </table>
</table>
<div id="log-container" style="height: 650px; overflow-y: scroll; background: #333; color: #aaa; padding: 10px;font-size: 16px;font-weight: bold">
    <div style="">
    </div>
</div>
</body>
<script>
    $(document).ready(function() {
        var id = '<%=request.getAttribute("id")%>' ;
        getserviceInfo(id);
        getprojectInfo(id)
        var t1,t2;
        window.clearTimeout(t1);//清除定时
        window.clearTimeout(t2);//清除定时
        t1 =  window.setInterval(function()
        {
            getserviceInfo (id);
        }, 3000);

        var t2 =  window.setInterval(function()
        {
            getprojectInfo (id);
        }, 3000);
        var websocket = new WebSocket('ws://localhost:8080/log/'+id);
        websocket.onmessage = function(event) {
            // 接收服务端的实时日志并添加到HTML页面中
            $("#log-container div").append(event.data);
            // 滚动条滚动到最低部
            $("#log-container").scrollTop($("#log-container div").height() - $("#log-container").height());
        };
    });
    /**
     *拿取服务器信息
     */
    function getserviceInfo(id) {

        $.get("/serverInfo/hardwareInfo?id="+id,function(result){
            if(result != null){
                $("#cpuinfo").text(result.cupRate)
                $("#memoryinfo").html("<font color='red'>"+result.memUsed+"</font> / "+result.memTotal+" / <font color='#D7D01D'>"+result.memRate+"</font> ")
                $("#deskinfo").html("<font color='red'>"+result.diskUsed+"</font> / "+result.diskTotal+" / <font color='#D7D01D'>"+result.diskRate+"</font> ")
            }else{
                $("#cpuinfo").text("获取失败")
                $("#memoryinfo").html("获取失败")
                $("#deskinfo").html("获取失败")
            }
        });
    }
    /**
     * 拿取项目运行情况
     */
    function getprojectInfo(id) {
        $.get("/serverInfo/project?id="+id,function(result){
            if(result != null && result.success){
                $("#status").css("color","#00a157")
               $("#status").html("<i class='fa fa-check-circle' aria-hidden='true'></i>")
            }else{
                $("#status").css("color","red")
                $("#status").html("<i class='glyphicon glyphicon-remove' aria-hidden='true'></i>")
            }
        });

    }
</script>
</body>
</html>