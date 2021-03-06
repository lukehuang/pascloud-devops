
var toolbar = function(){
	return [{
		text : '添加',  
        iconCls : 'icon-add',  
        handler : function(){
        	addDialog();
        }
	},{
		text : '复制',  
        iconCls : 'icon-table_edit',  
        handler : function(){
            alert('复制')
        }
	},{
		text : '运行',  
        iconCls : 'icon-table_edit',  
        handler : function(){
            startContainer();
        }
	},{
		text : '暂停',  
        iconCls : 'icon-table_edit',  
        handler : function(){
        	pauseContainer();
        }
	},{
		text : '恢复',  
        iconCls : 'icon-table_edit',  
        handler : function(){
        	unpauseContainer();
        }
	},{
		text : '停止',  
        iconCls : 'icon-table_edit',  
        handler : function(){
        	stopContainer();
        }
	},{
		text : '重启',  
        iconCls : 'icon-table_edit',  
        handler : function(){
        	restartContainer();
        }
	},{
		text : '销毁',  
        iconCls : 'icon-table_edit',  
        handler : function(){
            alert('销毁')
        }
	},{
		text : '查看配置文件',  
        iconCls : 'icon-table_edit',  
        handler : function(){
        	var row = $('#mainDataGrid').datagrid('getSelected'); 
        	//alert(row.name);
        	readSpringXml();
        }
	},{
		text : '查看日志',  
        iconCls : 'icon-table_edit',  
        handler : function(){
        	getContainerLog();
        }
	}];
}();