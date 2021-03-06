function addPasService(){
	var div = '';
	
	div +='<div style="margin:10px 0;width:100%;">&nbsp;';  
	div +='</div>';
	
	div +='<div style="margin:5px 0;width:100%;">';  
	div +=    '<label for="ip" class="formlabel">选择服务器IP:</label>';
	div +=    '<input class="easyui-combobox" id="ip" name="ip" data-options="valueField:\'ip\',textField:\'ip\',url:\'/module/server/appservers.json\'" >';
	div +=    '<div style="clear:both;"></div>';
	div +='</div>';
	
	div +='<div style="margin:5px 0;width:100%;">';  
	div +=    '<label for="type" class="formlabel">选择服务类别:</label>';
	div +=    '<input class="easyui-combobox" id="type" name="type" data-options="valueField:\'key\',textField:\'value\',url:\'/module/pasService/getPasCloudServiceType.json\'" >';
	div +=    '<div style="clear:both;"></div>';
	div +='</div>';
	
	div +='<div style="margin:5px 0;width:100%;">';  
	div +=    '<label for="clientIp" class="formlabel">代理ip地址:</label>';
	div +=    '<input class="easyui-validatebox formInput" id="clientIp" name="clientIp" data-options="required:false" value=""  >';
	div +=    '<div style="clear:both;"></div>';
	div +='</div>';
	
	
	div += createFormFooter(); 
	createDialogDivWithSize('mainDataGrid', 'addPasService','添加云服务', '',500,240,div);
}

function createFormFooter(){
	var html ="";
    html += '<div style="border:#ccc 0px solid;margin-bottom:25px;width:95%;line-height:24px;margin-top:10px;">';
    html +=   '<div style="float:left;width:25%;">';
	html +=   '&nbsp;'; 
	html +=   '</div>';
	html +=   '<div style="float:left;width:30%;">';
	html +=   '<a href="#" class="easyui-linkbutton" data-options="iconCls:\'icon-database_save\'" onclick="submit()" >确定</a>'; 
	html +=   '</div>';
	html +=   '<div style="border:#ccc 0px solid;float:left;width:30%;">';
	html +=   '<a href="#" class="easyui-linkbutton" data-options="iconCls:\'icon-2013040601125064_easyicon_net_16\'">重置</a>'; 
	html +=   '</div>';
	html +=   '<div style="clear:both;"></div>';
	html += '</div>';
	
	return html;
}


function submit(){
	var ip = $("#ip").combobox('getValue');
	var type = $("#type").combobox('getValue');
	var clientIp = $("#clientIp").val();
	
	if(type == 9){
		var regIP= /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
		if(!regIP.test(clientIp)){
			$.messager.alert('提示','ip格式必须为x.x.x.x x为数字');
			return ;
		}
	}
	
	
	
	var param = {ip:ip,type:type,clientIp:clientIp};
	if("" == ip || ip.length == 0 || "" == type || type.length == 0){
		$.messager.alert('提示','IP和类型不能为空');
		return ;
	}else{
		$('#addPasService').dialog('close');
		EasyUILoad('mainCenter');
		$.post("addPasService.json",param,function(data,status){
			
			if(data.code == 10000){
				
				$('#mainDataGrid').datagrid('reload');//刷新
				dispalyEasyUILoad( 'mainCenter' );
				$.messager.alert('提示','成功');
			}else{
				dispalyEasyUILoad( 'mainCenter' );
				$.messager.alert('提示',data.desc);
			}
			
		});
	}
}

function addContainer(){
	
	var nameVal = $("#name").val();
	var ipVal = $("#node").val();
	var portVal = $("#port").val();
	var imageNameVal = $("#imageName").val();
	
	var param = {name:nameVal,ip:ipVal,imageName:imageNameVal,port:portVal};
	EasyUILoad('mainCenter');
	$.post("addContainerForPB.json",param,function(data,status){
		if(data.code == 10000){
			$('#pasAddContainer').dialog('close');
			$('#mainDataGrid').datagrid('reload');//刷新
			dispalyEasyUILoad( 'mainCenter' );
		}
		
	});
}

function addBaseContainer(){
	
	var param = {};
	
	$.post("addBaseContainer.json",param,function(data,status){
		if(data.code == 10000){
			$('#mainDataGrid').datagrid('reload');//刷新
		}
		
	});
}

function addMainServiceContainer(){
	
	var param = {};
	
	$.post("addMainServiceContainer.json",param,function(data,status){
		if(data.code == 10000){
			$('#mainDataGrid').datagrid('reload');//刷新
		}
		
	});
}

function addPaspmServiceContainer(){
	
	var param = {};
	
	$.post("addPaspmServiceContainer.json",param,function(data,status){
		if(data.code == 10000){
			$('#mainDataGrid').datagrid('reload');//刷新
		}
		
	});
}

function addTomcatContainer(){
	
	var param = {};
	
	$.post("addTomcatContainer.json",param,function(data,status){
		if(data.code == 10000){
			$('#mainDataGrid').datagrid('reload');//刷新
		}
		
	});
}


function copyMainServiceContainer(){
	
	var param = {};
	
	$.post("copyMainServiceContainer.json",param,function(data,status){
		if(data.code == 10000){
			$('#mainDataGrid').datagrid('reload');//刷新
		}
		
	});
}

function copyPaspmServiceContainer(){
	
	var param = {};
	
	$.post("copyPaspmServiceContainer.json",param,function(data,status){
		if(data.code == 10000){
			$('#mainDataGrid').datagrid('reload');//刷新
		}
		
	});
}


function startContainer(){
	
	var row = $('#mainDataGrid').datagrid('getSelected'); 
	if(null==row){
		$.messager.alert('提示','请选择要重启的服务');
		return ;
	}
	
	var containerId = row.id;
	var ip = row.ip;
	var name = row.name;
	var type = row.type;
	var params = {containerId:containerId,ip:ip,name:name,type:type}

	if(row.state != 'running'){
		EasyUILoad('mainCenter');
		$.post("/module/container/startContainer.json",params,function(data,status){

			if(data.code == 10000){
				$('#mainDataGrid').datagrid('reload');//刷新
				dispalyEasyUILoad( 'mainCenter' );
				$.messager.alert('提示','成功');
			}else{
				dispalyEasyUILoad( 'mainCenter' );
				$.messager.alert('提示',data.desc);
			}
			
		});
	}else{
		$.messager.alert('提示','已经运行');
	}
}

function stopContainer(){
	
	var row = $('#mainDataGrid').datagrid('getSelected'); 
	
	if(null!=row){
		if(row.name == "shipyard-proxy"){
			$.messager.alert('提示','基础服务不能停止');
			return ;
		}
	}else{
		$.messager.alert('提示','请先选择一行');
		return ;
	}
	if(row.state == 'running'){
		EasyUILoad('mainCenter');
		$.post("/module/container/stopContainer.json",{containerId:row.id,ip:row.ip},function(data,status){
			if(data.code == 10000){
				
				
				$('#mainDataGrid').datagrid('reload');//刷新
				dispalyEasyUILoad( 'mainCenter' );
				$.messager.alert('提示','成功');
			}else{
				$.messager.alert('提示','失败');
			}
			
		});
	}else{
		$.messager.alert('提示','已经运行');
	}
}

function restartContainer(){
	
	var row = $('#mainDataGrid').datagrid('getSelected'); 
	if(null==row){
		$.messager.alert('提示','请选择要重启的服务');
		return ;
	}
	
	var containerId = row.id;
	var ip = row.ip;
	var name = row.name;
	var type = row.type;
	var params = {containerId:containerId,ip:ip,name:name,type:type}
	
	if(row.state == 'exited'){
		EasyUILoad('mainCenter');
		$.post("/module/container/restartContainer.json",params,function(data,status){
            if(data.code == 10000){
				
				$('#mainDataGrid').datagrid('reload');//刷新
				dispalyEasyUILoad( 'mainCenter' );
				$.messager.alert('提示','成功');
			}else{
				dispalyEasyUILoad( 'mainCenter' );
				$.messager.alert('提示','失败');
			}
		});
	}else{
		$.messager.alert('提示','先停止掉容器');
	}
}


function pauseContainer(){
	
	var row = $('#mainDataGrid').datagrid('getSelected'); 
	if(null!=row){
		if(row.name == "shipyard-proxy"){
			$.messager.alert('提示','基础服务不能停止');
			return ;
		}
	}else{
		$.messager.alert('提示','请先选择一行');
		return ;
	}
	if(row.state != 'paused'){
		EasyUILoad('mainCenter');
		$.post("/module/container/pauseContainer.json",{containerId:row.id,ip:row.ip},function(data,status){
            
			$('#mainDataGrid').datagrid('reload');//刷新
			dispalyEasyUILoad( 'mainCenter' );
			$.messager.alert('提示','成功');
		});
	}else{
		$.messager.alert('提示','已经暂停');
	}
}

function unpauseContainer(){
	
	var row = $('#mainDataGrid').datagrid('getSelected'); 
	if(row.state != 'running'){
		EasyUILoad('mainCenter');
		$.post("/module/container/unpauseContainer.json",{containerId:row.id,ip:row.ip},function(data,status){
			$('#mainDataGrid').datagrid('reload');//刷新
			dispalyEasyUILoad( 'mainCenter' );
			$.messager.alert('提示','成功');
		});
	}else{
		$.messager.alert('提示','已经恢复');
	}
}

function removeContainer(){
	
	var row = $('#mainDataGrid').datagrid('getSelected'); 
	
	if(null!=row){
		if(row.name!="" && row.name=='shipyard-proxy'){
			$.messager.alert('提示','基础服务不能销毁');
			return ;
		}
	}else{
		$.messager.alert('提示','请先选择一行');
		return ;
	}
	
	if(row.state == 'exited'|| row.state == 'created'){
		EasyUILoad('mainCenter');
		
		
		
		$.post("/module/container/removeContainer.json",{containerId:row.id,ip:row.ip},function(data,status){
			$('#mainDataGrid').datagrid('reload');//刷新
			dispalyEasyUILoad( 'mainCenter' );
			$.messager.alert('提示','成功');
		});
	}else{
		$.messager.alert('提示','先停止掉容器');
	}
}

function getContainerLog(){
	var row = $('#mainDataGrid').datagrid('getSelected'); 
	$.post("/module/container/getContainerLog.json",{name:row.name,ip:row.ip},function(data,status){
		//alert(data.desc);
		//$('#mainDataGrid').datagrid('reload');//刷新
		if(data.code = 10000){
		    //alert(data.desc);
			$('#pasSpringlog_text').val(data.desc);
			//$('#pasSpringlog_text').format({method: 'json'});
			$('#pasSpringlog').dialog('open');
		}
	});
}