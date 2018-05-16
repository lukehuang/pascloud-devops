package com.pascloud.module.mycat.web;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.pascloud.bean.docker.ContainerVo;
import com.pascloud.bean.docker.NodeVo;
import com.pascloud.bean.mycat.DataHostVo;
import com.pascloud.bean.mycat.DataNodeVo;
import com.pascloud.bean.pasdev.PasfileVo;
import com.pascloud.module.common.web.BaseController;
import com.pascloud.module.config.PasCloudConfig;
import com.pascloud.module.docker.service.DockerService;
import com.pascloud.module.mycat.service.MycatService;
import com.pascloud.utils.PasCloudCode;
import com.pascloud.utils.ScpClientUtils;
import com.pascloud.vo.result.ResultBean;
import com.pascloud.vo.result.ResultCommon;
import com.spotify.docker.client.DefaultDockerClient;


@Controller
@RequestMapping("module/mycat")
public class MycatController extends BaseController {
	@Autowired
	private MycatService m_mycatService;
	
	@Autowired
	private PasCloudConfig m_config;
	
	@Autowired
	private DockerService m_dockerService;
	
    private String mycat_schema_name = "schema.xml";
	
	private String mycat_server_name = "server.xml";
	
	@RequestMapping("/index.html")
	public ModelAndView index(HttpServletRequest request){
		ModelAndView view = new ModelAndView("mycat/index");
		return view;
	}
	
	@RequestMapping("/datanodes.json")
	@ResponseBody
	public List<DataNodeVo> getDataNodes(HttpServletRequest request){
		List<DataNodeVo> result = new ArrayList<>();
		result = m_mycatService.getDataNodes();
		return result;
	}
	
	@RequestMapping("/datanode.json")
	@ResponseBody
	public ResultBean<DataNodeVo> getDataNodeByName(HttpServletRequest request,
			@RequestParam(value="name",defaultValue="",required=true) String name){
		ResultBean<DataNodeVo> result = new ResultBean<>(PasCloudCode.SUCCESS);
		DataNodeVo dn = new DataNodeVo();
		
		if(null!=name && !"".equals(name)){
			dn = getDataNodeByName(name);
			result.setBean(dn);
		}
		return result;
	}
	
	@RequestMapping("addDatanode.json")
	@ResponseBody
	public ResultCommon addDatanode(HttpServletRequest request,
			@RequestParam(value="ip",defaultValue="",required=true) String ip,
			@RequestParam(value="port",defaultValue="",required=true) Integer port,
			@RequestParam(value="name",defaultValue="",required=true) String name,
			@RequestParam(value="dbType",defaultValue="",required=true) String dbType,
			@RequestParam(value="database",defaultValue="",required=true) String database,
			@RequestParam(value="username",defaultValue="",required=true) String user,
			@RequestParam(value="password",defaultValue="",required=true) String password){
		
		ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		
		m_mycatService.addDatanode(name, dbType, ip, user, password, database, port);
		
		return result;
		
	}
	
	@RequestMapping("delDatanode.json")
	@ResponseBody
	public ResultCommon delDatanode(HttpServletRequest request,
			@RequestParam(value="name",defaultValue="",required=true) String name){
		
		ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		m_mycatService.delDatanode(name);
		return result;
		
	}
	
	private DataNodeVo getDataNodeByName(String name){
		List<DataNodeVo> result = new ArrayList<>();
		result = m_mycatService.getDataNodes();
		
		if(result.size()>0){
			Iterator<DataNodeVo> it = result.iterator();
			while(it.hasNext()){
				DataNodeVo o = (DataNodeVo) it.next();	
				if(o.getName().equals(name)){
					return o;
				}
			}
		}
		return null;
		
	}
	
	@RequestMapping("uploadConfig.json")
	@ResponseBody
	public ResultCommon uploadConfig(HttpServletRequest 
			request){
		ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		List<ContainerVo> containers = new ArrayList<>();
		//containers = m_dockerService.getContainer(dockerClient);
		String mycat_schema_path = m_config.getPASCLOUD_MYCAT_DIR()+File.separator+this.mycat_schema_name;
		String mycat_server_path = m_config.getPASCLOUD_MYCAT_DIR()+File.separator+this.mycat_server_name;
		List<NodeVo> nodes = new ArrayList<>();
		
		nodes = m_dockerService.getNodes(dockerClient);
		/****查询运行的服务***/
		for(NodeVo vo: nodes){
			DefaultDockerClient client = DefaultDockerClient.builder()
					.uri("http://"+vo.getAddr()+":"+defaultPort).build();
			List<ContainerVo> subContainers = m_dockerService.getContainer(client,"pascloud_mycat");
			if(null != subContainers && subContainers.size()>0){
				containers.addAll(subContainers);
			}
		}
		/****上传到复制的项目***/
		if(containers.size()>0){
			for(ContainerVo vo : containers){
				ScpClientUtils scpClient = new ScpClientUtils(vo.getIp(), "root", "tccp@2012");
				
				
				String path = "/home/pascloud/mycat/conf/";
				scpClient.putFileToServer(mycat_schema_path, path);
				scpClient.putFileToServer(mycat_server_path, path);
				scpClient.close();
			}
		}
		
		
		return result;
		
	}
	
	

}
