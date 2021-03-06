package com.pascloud.module.server.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pascloud.module.common.web.BaseController;
import com.pascloud.module.docker.service.DockerService;
import com.pascloud.module.server.service.ServerService;
import com.pascloud.utils.PasCloudCode;
import com.pascloud.utils.ScpClientUtils;
import com.pascloud.vo.common.MapVo;
import com.pascloud.vo.database.DBInfo;
import com.pascloud.vo.docker.NodeVo;
import com.pascloud.vo.result.ResultCommon;
import com.pascloud.vo.server.ServerVo;
import com.pascloud.vo.server.SysServerInfo;

@Controller
@RequestMapping("module/server")
public class ServerController extends BaseController {
	
	@Autowired
	private DockerService m_dockerService;
	
	@Autowired
	private ServerService m_serverService;
	
	@RequestMapping("index.html")
	public String index(HttpServletRequest request){
		return "server/index";
	}
	
	@RequestMapping("servers.json")
	@ResponseBody
	public List<ServerVo> getServers(){
		
		List<ServerVo> result = new ArrayList<>();
		result = m_serverService.getServers();
		return result;
	}
	
	@RequestMapping("appservers.json")
	@ResponseBody
	public List<ServerVo> getAppServers(){
		
		List<ServerVo> result = new ArrayList<>();
		result = m_serverService.getAppServers();
		return result;
	}
	
	@RequestMapping("getServerInfo")
	@ResponseBody
	public List<MapVo> getServerInfo(HttpServletRequest request,
			@RequestParam(value="index",defaultValue="",required=true) int index){
		
		List<NodeVo> nodes = new ArrayList<>();
		nodes = m_dockerService.getNodes(getDockerClient());
		List<MapVo> maps = new ArrayList<>();
		ServerVo server = m_serverService.getByIP(nodes.get(index).getAddr());
		if(null!=server){
			ScpClientUtils client = new ScpClientUtils(nodes.get(index).getAddr(),server.getUsername(),server.getPassword());
			SysServerInfo info = new SysServerInfo();
			info = client.getServerInfo();
			client.close();
			
			MapVo vo = new MapVo();
			vo.setKey("服务器名称：");
			vo.setValue(nodes.get(index).getHostname());
			maps.add(vo);
			
			MapVo vo1 = new MapVo();
			vo1.setKey("服务器IP：");
			vo1.setValue(nodes.get(index).getAddr());
			maps.add(vo1);
			
			MapVo vo2 = new MapVo();
			vo2.setKey("CPU使用率：");
			vo2.setValue(info.getCpu_used());
			maps.add(vo2);
			
			MapVo vo3 = new MapVo();
			vo3.setKey("总内存大小：");
			vo3.setValue(info.getMemory_total());
			maps.add(vo3);
			
			MapVo vo4 = new MapVo();
			vo4.setKey("内存剩余大小：");
			vo4.setValue(info.getMemory_free());
			maps.add(vo4);
			
			MapVo vo6 = new MapVo();
			vo6.setKey("内存使用率：");
			vo6.setValue(info.getMemory_used());
			maps.add(vo6);
			
			MapVo vo5 = new MapVo();
			vo5.setKey("服务器OS：");
			vo5.setValue(info.getOs());
			maps.add(vo5);
		}
		
		
		return maps;
	}
	
	@RequestMapping(value="addServer",method=RequestMethod.POST)
	@ResponseBody
	public ResultCommon addServer(HttpServletRequest request,
			@RequestParam(value="ip",defaultValue="",required=true) String ip,
			@RequestParam(value="user",defaultValue="",required=true) String user,
			@RequestParam(value="password",defaultValue="",required=true) String password,
			@RequestParam(value="port",defaultValue="0",required=true) Integer port,
			@RequestParam(value="typeEnum",defaultValue="0",required=true) Integer typeEnum){
		
		
		ResultCommon result = null;
		
		if(ip.length() == 0 || user.length() == 0 || password.length() == 0 || port == 0 || typeEnum == 0){
			result = new ResultCommon(PasCloudCode.PARAMEXCEPTION);
			return result;
		}
		log.info("验IP是否重复");
		if(m_serverService.checkServerIsExist(ip,typeEnum+"")){
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),"该服务器已经存在。");
		}else{
			ServerVo vo = new ServerVo();
	    	vo.setIp(ip);
	    	vo.setUsername(user);
	    	vo.setPassword(password);
	    	vo.setPort(port+"");
	    	vo.setTypeEnum(typeEnum);
	    	vo.setType("");
	    	if(typeEnum == 1){
	    		vo.setDesc("应用服务器");
	    	}else if(typeEnum == 2){
	    		vo.setDesc("数据库服务器");
	    	}
	    	log.info("添加服务器");
	    	if(m_serverService.addServer(vo)){
	    		result = new ResultCommon(PasCloudCode.SUCCESS);
	    	}else{
	    		result = new ResultCommon(PasCloudCode.ERROR);
	    	}
		}
		
		return result;
	}
	
	@RequestMapping(value="delServer",method=RequestMethod.POST)
	@ResponseBody
	public ResultCommon delServer(HttpServletRequest request,
			@RequestParam(value="ip",defaultValue="",required=true) String ip){
		
		
		ResultCommon result = null;
		
		if(ip.length() == 0 ){
			result = new ResultCommon(PasCloudCode.PARAMEXCEPTION);
			return result;
		}
		log.info("检查IP是否存在");
		if(m_serverService.checkServerIsExist(ip)){
			if(m_serverService.delServer(ip)){
	    		result = new ResultCommon(PasCloudCode.SUCCESS);
	    	}else{
	    		result = new ResultCommon(PasCloudCode.ERROR);
	    	}
		}else{
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),"该服务器不存在。");
		}
		
		return result;
	}

}
