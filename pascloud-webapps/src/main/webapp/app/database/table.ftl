<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title></title>
	<link rel="stylesheet" type="text/css" href="/static/easyui/themes/bootstrap/easyui.css">
    <link rel="stylesheet" type="text/css" href="/static/easyui/themes/icon.css">
    <link rel="stylesheet" type="text/css" href="/static/easyui/themes/IconExtension.css">
    
    <link id="themesUI" href="/static/css/jquery-ui-1.9.2.custom.min.css" rel="stylesheet"  type="text/css"/>
   
    
	<script type="text/javascript" src="/static/easyui/jquery-1.8.0.min.js"></script>
    <script type="text/javascript" src="/static/easyui/jquery.easyui.min.js"></script>
    
    <script type="text/javascript" src="/static/js/lib/jquery.format.js"></script>
    
    <script type="text/javascript" src="/static/js/common/pascloudfunctions.js"></script>
    
    
	<script type="text/javascript">
	</script>
	<style>
	    .datagrid-btable .datagrid-cell{padding:6px 4px;overflow: hidden;text-overflow:ellipsis;white-space: nowrap;}  
	    .formlabel{width:30%;text-align:right;float:left;}
	    .formInput{float:left;margin-left:10px;}
	    .border_right{border-right:#ccc 1px solid;}
	    .border_bottom{border-bottom:#ccc 1px solid;}
	</style>
	
	
</head>
<body id="main" class="easyui-layout" data-options="fit:true,border:false" > 

    <div id="mainCenter" data-options="region:'center'" style="padding:0px;">
		<!--内容  开始-->
		<div id="mainGridLayout" class="easyui-layout" data-options="fit:true,title:'首页',iconCls:'icon-house'">
		    <div data-options="region:'center'">
		        <table id="tableDataGrid" class="easyui-datagrid" 
				    data-options="url:'${url}',fitColumns:true,singleSelect:true,
				    iconCls:'icon-table',pagination:true,fit:true,
				    pageSize:20,pageList:[20, 50, 100, 150, 200],border:0" >
                    <thead>
		                <tr>
		                <#list columns as col> 
			            <th data-options="field:'${col.columnName}',width:100">${col.columnName}</th>
			            </#list>
		                </tr>
                    </thead>
                </table>
		    </div>
		</div>
		<!--内容  结束-->
	</div>

</body>
</html>