function initMVersionMainTreeGrid(){
    EasyUILoad('mainCenter');
    $('#mVersionMainTreeGrid').treegrid({
	    url:'getMVersionMenuTree.json',
		method:'get',
		border:false,
		width: "auto",
		fit:true,
		queryParams:{'Id':'dn0'},
		animate:true,//是否用动画效果
        collapsible:false,//是否可折叠
        rownumbers:true,//行号
        fitColumns:true,
        idField:'id',//根据那个字段判断树节点关系
        treeField:'text',//根据那个列展现树
        showFooter:false,//是否使用页脚
		columns:[[
            //{title:'编号',field:'id',width:40,sortable:true},
            {title:'资源名称',field:'text',width:200,sortable:true},
            {title:'级别',field:'level',width:160,sortable:true,hidden:'true'},
            {title:'版本',field:'version',width:40,sortable:true},
            {title:'是否禁用',field:'sfxs',width:40,sortable:true,formatter:formatOper },
            {title:'类型',field:'type',width:40,sortable:true},
            {title:'地址',field:'url',width:160,sortable:true}
        ]],
        toolbar:mtoolbar,
		onContextMenu: onContextMenu,
		onClickRow: onClickRow,
		onLoadSuccess:function(node, data){
			dispalyEasyUILoad( 'mainCenter' );	
		},
		onDblClickCell:function(rowIndex, field, value){
			var node = $('#mVersionMainTreeGrid').treegrid('getSelected');
			var title = node.id;
			var index= node.id;
			var icon = "";
			//alert(title);
		}
	});
}

function formatOper(val,row,index){ 
	if (val == '0'){
		return '<span style="color:blue;">否</span>';
	} else {
		return '<span style="color:red;">是</span>';;
	} 
} 

function onContextMenu(e,node){
    e.preventDefault();
	$(this).treegrid('select',node.id);
	level = node.level;
	pId = node.id;
	$('#mm').menu('show',{
	    left: e.pageX,
	    top: e.pageY
    });
}

function onClickRow(node){
	
	level = node.level;
	pId = node.id;
	
}

function reloadTree(){
	$('#mVersionMainTreeGrid').treegrid('options').url ='getMVersionMenuTree.json?Id=dn0';
	$('#mVersionMainTreeGrid').treegrid("reload");
}



