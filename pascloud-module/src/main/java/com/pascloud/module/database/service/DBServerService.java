package com.pascloud.module.database.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.pascloud.constant.Constants;
import com.pascloud.module.common.service.AbstractBaseService;
import com.pascloud.module.config.PasCloudConfig;
import com.pascloud.module.server.service.ServerService;
import com.pascloud.utils.DBUtils;
import com.pascloud.utils.FileUtils;
import com.pascloud.utils.PasCloudCode;
import com.pascloud.vo.database.DBInfo;
import com.pascloud.vo.database.DBUser;
import com.pascloud.vo.mversion.XtcdVo;
import com.pascloud.vo.result.ResultCommon;
import com.pascloud.vo.script.ScriptEnum;
import com.pascloud.vo.server.ServerVo;
import com.pascloud.vo.tenant.KhdxHyVo;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

@Service
public class DBServerService extends AbstractBaseService {

	@Autowired
	private ServerService  m_serverService;
	@Autowired
	private PasCloudConfig m_config;

	private static Logger log = LoggerFactory.getLogger(DBServerService.class);

	public List<DBInfo> getOracleSidWithServer(ServerVo server) {
		List<DBInfo> result = new ArrayList<>();
		List<String> lists = getOracleSid(server.getIp());
		if (null != lists && lists.size() > 0) {
			for (String s : lists) {
				DBInfo db = new DBInfo();
				if (s.contains("ora_pmon")) {
					s = s.replaceAll("ora_pmon_", "");
					db.setId(s);
					db.setName(s);
					db.setUsername("pas");
					db.setPassword("pas");
					db.setUrl(DBUtils.getUrlByParams(ScriptEnum.ORA.getValue(), server.getIp(), s, 1521));
					
					
					result.add(db);
				}
			}
		}
		return result;
	}
	
	public List<DBInfo> getOracleUserWithSid(String sid,String url,String username,String password) {
		List<DBInfo> result = new ArrayList<>();
		List<DBUser> users = new ArrayList<>();
		users = gerUserFromSid( sid, url, ScriptEnum.ORA.getValue(), username, password);
		if (null != users && users.size() > 0) {
			for (DBUser u : users) {
				DBInfo db = new DBInfo();
				db.setId(sid);
				db.setName(sid);
				db.setUsername(u.getUsername().toLowerCase());
				db.setPassword(u.getUsername().toLowerCase());
				db.setUrl(url);
				result.add(db);
			}
		}
		
		return result;
	}

	public List<String> getOracleSid(String ip) {
		Connection conn = null;
		List<String> lists = new ArrayList<>();
		StringBuffer sb = new StringBuffer();
		InputStream stdout = null;
		StringTokenizer tokenStat = null;
		Session session = null;
		BufferedReader br = null;
		int i = 0;
		try {
			ServerVo vo = m_serverService.getByIP(ip);
			// SCPClient scpClient = conn.createSCPClient();
			if (null != vo) {
				conn = getScpClientConn(vo.getIp(), vo.getUsername(), vo.getPassword());
				session = conn.openSession();
				session.execCommand("ps -ef | grep ora_pmon");
				stdout = new StreamGobbler(session.getStdout());
				br = new BufferedReader(new InputStreamReader(stdout));
				while (true) {
					String line = br.readLine();
					if (line == null)
						break;
					if (line.contains(ScriptEnum.ORA.getValue())) {
						tokenStat = new StringTokenizer(line);
						for (i = 0; i < 7; i++) {
							tokenStat.nextToken();
						}
						lists.add(tokenStat.nextToken());
					} else {
						continue;
					}
					log.info(line);
					sb.append(line);
				}
			}

		} catch (IOException e) {
			log.error(e.getMessage());
		} finally {
			// session.close();
			try {
				stdout.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				log.error(e1.getMessage());
			}
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
			}
			session.close();
			conn.close();
		}

		return lists;
	}

	/**
	 * 创建数据库实例
	 * @param ip
	 * @param sid
	 * @param tnsnamePath
	 * @return
	 */
	public ResultCommon createOracleWithSid(String ip, String sid, String tnsnamePath) {
		ResultCommon result = null;
		Boolean flag = false;
		log.info("ip:" + ip + ",sid:" + sid);
		result = createOracleBySID(sid, ip, tnsnamePath);
		return result;
	}
	
	
	/**
	 * 创建数据库SCHEMA
	 * @param ip
	 * @param sid
	 * @param username
	 * @param password
	 * @param url
	 * @return
	 */
	public ResultCommon createOracleWithUser(String ip, String sid, String username,
			String password,String url,String dbUser ,String dbPass) {
		ResultCommon result = null;
		Boolean flag = false;
		Session session = null;
		Connection conn = null;
		try {
			if (null != ip) {
				ServerVo vo = m_serverService.getByIP(ip);
				conn = getScpClientConn(ip, vo.getUsername(), vo.getPassword());
				
				
				
				if(!checkDirIsExist(conn, m_config.getORACLE_DBHOME())){
					result = new ResultCommon(PasCloudCode.ERROR.getCode(),m_config.getORACLE_DBHOME()+"目录不存在");
					return result;
				}
				
				if(checkSchemaIsExists(sid,url,ScriptEnum.ORA.getValue(),username)>0){
					result = new ResultCommon(PasCloudCode.ERROR.getCode(),m_config.getORACLE_DBHOME()+"该用户已经存在");
					return result;
				}
				
				if (grantWithSidAndUser(conn, sid,username,password,dbUser,dbPass)) {
					if (grantWithSidAndUserShell(conn, sid,username,password)) {
						if (impDmpFileWithSidAndUser(conn, sid,username,password)) {
							result = new ResultCommon(PasCloudCode.SUCCESS);
						}else{
							result = new ResultCommon(PasCloudCode.ERROR.getCode(),"impDmpFileWithSidAndUser失败");
						}
					}else{
						result = new ResultCommon(PasCloudCode.ERROR.getCode(),"grantWithSidAndUserShell失败");
					}
				}else{
					result = new ResultCommon(PasCloudCode.ERROR.getCode(),"grantWithSidAndUser失败");
				}
				
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),e.getMessage());
			log.error(e.getMessage());
		} finally {
			if(null!=session){
				session.close();
			}
			if(null!=conn){
				conn.close();
			}
		}
		return result;
	} 
	
	
	public ResultCommon createOracleWithUser(Connection conn, String sid, String username,
			String password,String dbUser ,String dbPass) {
		ResultCommon result = null;
		Boolean flag = false;
		Session session = null;
		try {
			if (null != conn) {
				
				if(!checkDirIsExist(conn, m_config.getORACLE_DBHOME())){
					result = new ResultCommon(PasCloudCode.ERROR.getCode(),m_config.getORACLE_DBHOME()+"目录不存在");
					return result;
				}
				//
				
				if (grantWithSidAndUser(conn, sid,username,password,dbUser,dbPass)) {
					if (grantWithSidAndUserShell(conn, sid,username,password)) {
						if (impDmpFileWithSidAndUser(conn, sid,username,password)) {
							result = new ResultCommon(PasCloudCode.SUCCESS);
						}else{
							result = new ResultCommon(PasCloudCode.ERROR.getCode(),"impDmpFileWithSidAndUser失败");
						}
					}else{
						result = new ResultCommon(PasCloudCode.ERROR.getCode(),"grantWithSidAndUserShell失败");
					}
				}else{
					result = new ResultCommon(PasCloudCode.ERROR.getCode(),"grantWithSidAndUser失败");
				}
				
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),e.getMessage());
			log.error(e.getMessage());
		} finally {
			if(null!=session){
				session.close();
			}
		}
		return result;
	} 
	

	private ResultCommon createOracleBySID(String sid, String ip, String tnsnamePath) {
		Boolean flag = false;
		ResultCommon result = null;
		StringBuffer sb = new StringBuffer();
		InputStream stdout = null;
		Session session = null;
		BufferedReader br = null;
		int i = 0;
		Connection conn = null;
		Connection conn2 = null;
		try {
			if (null != ip) {
				ServerVo vo = m_serverService.getByIP(ip);
				//conn2 = getScpClientConn(ip, "oracle", "oracle");
				//conn = getScpClientConn(ip, vo.getUsername(), vo.getPassword());
				conn = getScpClientConn(ip, vo.getUsername(), vo.getPassword());
				if(!checkDirIsExist(conn, m_config.getORACLE_DBHOME())){
					result = new ResultCommon(PasCloudCode.ERROR.getCode(),m_config.getORACLE_DBHOME()+"目录不存在");
					return result;
				}
				
				session = conn.openSession();
				session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/create_database.sh" + " " + sid+" "+m_config.getORACLE_DBHOME()+"/bin/dbca");
				stdout = new StreamGobbler(session.getStdout());
				br = new BufferedReader(new InputStreamReader(stdout));
				while (true) {
					String line = br.readLine();
					if (line == null) {
						flag = true;
						break;
					} else {
						log.info(line);
					}
					sb.append(line);
				}
				//System.out.println(flag);

				if (flag) {
                    /* 
					if (changeScriptOwn(conn)) {
						flag = changeScriptMod(conn);
					} else {
						flag = false;
						result = new ResultCommon(PasCloudCode.ERROR.getCode(),"更改权限changeScriptOwn失败");
					}*/

					if (flag) {
						if (addSidWithTnsname(sid, conn, ip, tnsnamePath)) {
							// flag =
							// impDmpFileWithSid(conn2,sid);createTableSpaceWithSid
							if(addSidWithListener(sid,conn,ip)){
								
								if (createTableSpaceWithSid(conn, sid)) {
									/*
									if (grantWithSid(conn, sid)) {
										if(restartListenerWithSid(conn, sid)){
											
											result = new ResultCommon(PasCloudCode.SUCCESS);
										}else{
											result = new ResultCommon(PasCloudCode.ERROR.getCode(),"restartListenerWithSid失败");
										}
									}else{
										result = new ResultCommon(PasCloudCode.ERROR.getCode(),"grantWithSid失败");
									}*/
									
									if(createManagerUserWithSid(sid,conn)){
										result= createOracleWithUser(conn, sid,"pas", "pas", "CLOUDMANAGER", "CLOUDMANAGER");
											
										
									}else{
										result = new ResultCommon(PasCloudCode.ERROR.getCode(),"createManagerUserWithSid创建管理员失败");
									}
									
								}else{
									result = new ResultCommon(PasCloudCode.ERROR.getCode(),"createTableSpaceWithSid失败");
								}
							}else{
								result = new ResultCommon(PasCloudCode.ERROR.getCode(),"addSidWithListener失败");
							}
						}else{
							result = new ResultCommon(PasCloudCode.ERROR.getCode(),"addSidWithTnsname失败");
						}
						
					}else{
						result = new ResultCommon(PasCloudCode.ERROR.getCode(),"更改权限changeScriptMod失败");
					}
				}
			} else {
				result = new ResultCommon(PasCloudCode.ERROR.getCode(),"执行创建脚本create_database.sh失败");
				return result;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),e.getMessage());
			log.error(e.getMessage());
		} finally {
			// session.close();
			try {
				if(null!=stdout){
					stdout.close();
				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				log.error(e1.getMessage());
			}
			try {
				if(null!=br){
					br.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
			}
			if(null!=session){
				session.close();
			}
			if(null!=conn){
				conn.close();
			}
		}

		return result;
	}
	
	public Boolean createManagerUserWithSid(String sid,Connection conn) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		//Connection conn = ;
		try {
			log.info("新建管理员");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/create_cloudmanager.sh" + " " + sid +" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("管理员新建成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("新建管理员异常");
		} finally {
			
			
			
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}
	
	public Boolean createManagerUserWithSid(String sid,String ip) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		Connection conn = null;
		try {
			ServerVo vo = m_serverService.getByIP(ip);
			conn = getScpClientConn(ip, vo.getUsername(), vo.getPassword());
			log.info("新建管理员");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/create_cloudmanager.sh" + " " + sid +" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("管理员新建成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("新建管理员异常");
		} finally {
			
			
			
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
			if(null!=conn){
				conn.close();
			}
		}
		return flag;
	}

	private Boolean createTableSpaceWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		//InputStream stderr = null;
		BufferedReader br = null;
		//BufferedReader br2 = null;
		try {
			log.info("新建表空间");
			session = conn.openSession();
			//log.info(Constants.LINUX_ORACLE_HOME_SCRIPT+"/create_tablespace.sh" + " " + sid);
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/create_tablespace.sh" + " " + sid+" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			//stderr = new StreamGobbler(session.getStderr());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				
				if (line == null) {
					log.info("新建表空间成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
		        //log.info(line);
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("新建表空间异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean grantWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("新建用户pas");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/grant_sid.sh" + " " + sid+" "+m_config.getORACLE_DBHOME());
			//session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/grant_sid.sh" + " " + sid+" "+"/u01/app/oracle/product/11.2.0/dbhome_1");
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("新建用户pas成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("新建用户pas异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}
	
	/**
	 * 
	 * @param conn
	 * @param sid
	 * @param username
	 * @param password
	 * @param dbUser
	 * @param dbPass
	 * @return
	 */
	private Boolean grantWithSidAndUser(Connection conn, String sid,String username,String password,
			String dbUser,String dbPass) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("新建用户"+username);
			session = conn.openSession();
			
			session.execCommand(
					Constants.LINUX_ORACLE_HOME_SCRIPT+
					"/grant_sidV3.sh" +
					" " + sid+
					" "+m_config.getORACLE_DBHOME()+
					" "+dbUser.toUpperCase()+
					" "+dbPass.toUpperCase()+
					" "+username.toLowerCase()+
					" "+password.toLowerCase());
			//session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/grant_sid.sh" + " " + sid+" "+"/u01/app/oracle/product/11.2.0/dbhome_1");
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("新建用户"+username+"成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("新建用户pas异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean grantWithSidAndUserShell(Connection conn, String sid,String username,String password) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("执行用户"+username+"脚本");
			session = conn.openSession();
			
			session.execCommand(
					Constants.LINUX_ORACLE_HOME_SCRIPT+
					"/create_shell.sh" +
					" " + sid+
					" "+m_config.getORACLE_DBHOME()+
					" "+username.toLowerCase()+
					" "+password.toLowerCase());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("执行用户脚本"+username+"成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("新建用户pas异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}
	
	public Boolean restartListenerWithSid(String sid,String ip) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		Connection conn = null;
		try {
			ServerVo vo = m_serverService.getByIP(ip);
			conn = getScpClientConn(ip, vo.getUsername(), vo.getPassword());
			log.info("重启监听");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/restart_listener.sh" + " " + sid +" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("重启监听成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("新建用户pas异常");
		} finally {
			
			
			
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
			if(null!=conn){
				conn.close();
			}
		}
		return flag;
	}
	
	private Boolean restartListenerWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("重启监听");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/restart_listener.sh" + " " + sid +" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("重启监听成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("新建用户pas异常");
		} finally {
			
			
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	public ResultCommon deleteOracleWithSid(String ip, String sid, String oratabfilePath, String tnsnamePath) {
		ResultCommon result = null;
		Boolean flag = false;
		Connection conn = null;
		try {
			log.info("删除数据库开始" + sid);
			ServerVo vo = m_serverService.getByIP(ip);
			if (null != vo) {
				conn = getScpClientConn(vo.getIp(), vo.getUsername(), vo.getPassword());
				if (shutDownOracleWithSid(conn, sid)) {
					if (deleteOracleWithSid(conn, sid)) {
						if (delSIDWithOratab(conn, sid, oratabfilePath)) {
							if(delSidWithListener(sid,conn,ip)){
								if(delSidWithTnsname(sid, conn, ip, tnsnamePath)){
									//flag = restartListenerWithSid(conn, sid);
									//if(flag){
									result = new ResultCommon(PasCloudCode.SUCCESS);
									//}else{
										//result = new ResultCommon(PasCloudCode.ERROR.getCode(),"restartListenerWithSid失败");
									//}
								}else{
									result = new ResultCommon(PasCloudCode.ERROR.getCode(),"delSidWithTnsname失败");
								}
								
							}else{
								result = new ResultCommon(PasCloudCode.ERROR.getCode(),"delSidWithListener失败");
							}
							
						}else{
							result = new ResultCommon(PasCloudCode.ERROR.getCode(),"delSIDWithOratab失败");
						}
					}else{
						result = new ResultCommon(PasCloudCode.ERROR.getCode(),"deleteOracleWithSid失败");
					}
				}else{
					result = new ResultCommon(PasCloudCode.ERROR.getCode(),"shutDownOracleWithSid失败");
				}
			}else{
				result = new ResultCommon(PasCloudCode.ERROR.getCode(),"找不到服务器");
			}
		} catch (Exception e) {
			log.info("删除数据库开始" + sid + "异常");
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),e.getMessage());
		} finally {
			if(null!=conn){
				conn.close();
			}
			
		}
		return result;
	}

	public Boolean startOracleWithSid(String sid) {
		Boolean flag = false;
		try {

		} catch (Exception e) {

		}

		return flag;
	}

	public Boolean importDmpfileWithSid(String ip, String sid) {
		Boolean flag = false;
		ServerVo vo = m_serverService.getByIP(ip);
		log.info("ip:" + ip + ",sid:" + sid);
		Connection conn = null;
		if (null != vo) {
			conn = getScpClientConn(vo.getIp(), vo.getUsername(), vo.getPassword());
			flag = impDmpFileWithSid(conn, sid);
		}

		return flag;
	}
	
	public Boolean importDmpfileWithSidAndUser(String ip,String sid,String username,String password,String url) {
		Boolean flag = false;
		ServerVo vo = m_serverService.getByIP(ip);
		log.info("ip:" + ip + ",sid:" + sid);
		Connection conn = null;
		if (null != vo) {
			conn = getScpClientConn(vo.getIp(), vo.getUsername(), vo.getPassword());
			flag = impDmpFileWithSidAndUser( conn,  sid, username, password);
		}

		return flag;
	}

	private Boolean impDmpFileWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("导入DMP文件");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/imp_dmp.sh" + " " + sid+" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("导入DMP文件成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
				System.out.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("导入DMP文件异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}
	
	private Boolean impDmpFileWithSidAndUser(Connection conn, String sid,String username,String password) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("导入DMP文件");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+
					"/imp_dmpV2.sh" +
					" " + sid+
					" "+m_config.getORACLE_DBHOME()+
					" "+username.toLowerCase()+
					" "+password.toLowerCase());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("导入DMP文件成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
				System.out.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("导入DMP文件异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean shutDownOracleWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("关闭数据库");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/shutdown.sh" + " " + sid+" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("关闭数据库成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("关闭数据库异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean deleteOracleWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("删除数据库文件");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/delete_database.sh" + " " + sid + " " + getSidEx(sid));
			stdout = new StreamGobbler(session.getStderr());
			br = new BufferedReader(new InputStreamReader(stdout));
			
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("删除数据库文件成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
			//return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("删除数据库文件异常");
			log.error(e.getMessage());
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean delSIDWithOratab(Connection conn, String sid, String oratabfilePath) {
		StringBuffer sb = new StringBuffer();
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("删除oratab文件里面的SID信息");
			session = conn.openSession();
			session.execCommand("cat " + "/etc/oratab");
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					flag = true;
					break;
				}
				if (!line.startsWith("#")) {
					if (line.contains(sid)) {
					} else {
						sb.append(line).append("\n");
					}
				}
			}
			if (flag) {
				File file = new File(oratabfilePath);
				if (!file.exists()) {
					FileUtils.createOrExistsFile(file);
				}
				FileUtils.writeFileFromString(file, sb.toString(), false);
				uploadOratabToServer(conn, oratabfilePath);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("删除oratab文件里面的SID信息异常");
			log.error(e.getMessage());
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private String getSidEx(String sid) {
		String ex = Constants.ORACLE_SID_EX_PREEFIX;
		String sidNum = sid.replace(Constants.ORACLE_SID_PREEFIX, "");
		ex = ex + sidNum + "*";
		log.info("正则表达式：" + ex);
		return ex;
	}

	private Boolean uploadOratabToServer(Connection conn, String oratabfilePath) {
		Boolean flag = false;
		SCPClient scpClient = null;
		try {
			log.info("上传" + oratabfilePath + "文件");
			scpClient = conn.createSCPClient();
			scpClient.put(oratabfilePath, "/etc/");
			flag = true;
			log.info("上传" + oratabfilePath + "文件成功");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("上传" + oratabfilePath + "文件异常");
		} finally {

		}
		return flag;
	}

	private Boolean uploadTnsnameToServer(Connection conn, String tnsnamePath) {
		Boolean flag = false;
		SCPClient scpClient = null;
		//String testpath = "/u01/app/oracle/product/11.2.0/dbhome_1";
		try {
			log.info("上传" + tnsnamePath + "文件");
			scpClient = conn.createSCPClient();
		    scpClient.put(tnsnamePath, m_config.getORACLE_DBHOME()+"/network/admin/");
			//scpClient.put(tnsnamePath, testpath+"/network/admin/");
			flag = true;
			log.info("上传" + tnsnamePath + "文件成功");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("上传" + tnsnamePath + "文件异常");
		} finally {

		}
		return flag;
	}

	private Boolean addSidWithTnsname(String sid, Connection conn, String ip, String tnsnamePath) {
		Boolean flag = false;
		InputStream stdout = null;
		BufferedReader br = null;
		Session session = null;
		StringBuffer sb = new StringBuffer();
		StringBuffer sidSb = new StringBuffer();
		Boolean isExist = false;
		//String testpath = "/u01/app/oracle/product/11.2.0/dbhome_1";
		try {
			log.info("读取tnsnames.ora文件");
			session = conn.openSession();
			session.execCommand("cat " + m_config.getORACLE_DBHOME()+"/network/admin/tnsnames.ora");
			//session.execCommand("cat " + testpath+"/network/admin/tnsnames.ora");
			
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			int i = 0;
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				if (line.startsWith(sid.toUpperCase())) {
					isExist = true;
				}
				sb.append(line).append("\n");
				i++;
			}

			if (!isExist) {
				log.info("编写" + sid + "内容");
				sidSb.append(sid.toUpperCase() + " =").append("\n").append("  (DESCRIPTION =").append("\n")
						.append("    (ADDRESS = (PROTOCOL = TCP)(HOST = " + ip + ")(PORT = 1521))").append("\n")
						.append("    (CONNECT_DATA =").append("\n").append("      (SERVER = DEDICATED)").append("\n")
						.append("      (SERVICE_NAME = " + sid.toLowerCase() + ")").append("\n").append("    )")
						.append("\n").append("  )").append("\n").append("").append("\n");
				if (!FileUtils.isFileExists(tnsnamePath)) {
					FileUtils.createOrExistsFile(tnsnamePath);
				}
				sb.append("").append("\n").append(sidSb.toString());
				log.info("向tnsnames.ora写入" + sid + "的配置内容");
				log.info(sb.toString());
				if (FileUtils.writeFileFromString(new File(tnsnamePath), sb.toString(), false)) {
					flag = uploadTnsnameToServer(conn, tnsnamePath);
				}

			} else {
				flag = true;
				log.info("tnsnames.ora文件已经配置了" + sid);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			log.info("发生异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			session.close();
		}

		return flag;
	}

	private Boolean delSidWithTnsname(String sid, Connection conn, String ip, String tnsnamePath) {
		Boolean flag = false;
		InputStream stdout = null;
		BufferedReader br = null;
		Session session = null;
		StringBuffer sb = new StringBuffer();
		Boolean isExist = false;
		try {
			log.info("读取tnsnames.ora文件");
			session = conn.openSession();
			session.execCommand("cat " + m_config.getORACLE_DBHOME()+"/network/admin/tnsnames.ora");
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			int i = 0;
			int index = 0;
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				if (line.startsWith(sid.toUpperCase())) {
					index = i;
					isExist = true;
				}
				if (index > 0 && i <= index + 7) {

				} else {
					sb.append(line).append("\n");
				}
				i++;
			}

			if (isExist) {

				if (!FileUtils.isFileExists(tnsnamePath)) {
					FileUtils.createOrExistsFile(tnsnamePath);
				}
				log.info("向tnsnames.ora写入" + sid + "的配置内容");
				FileUtils.writeFileFromString(new File(tnsnamePath), sb.toString(), false);
				flag = uploadTnsnameToServer(conn, tnsnamePath);
			} else {
				flag = true;
				log.info("tnsnames.ora文件没有配置" + sid);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			log.info("发生异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			session.close();
		}

		return flag;
	}

	/**
	 * listener.ora
	 * 
	 * @param sid
	 * @param conn
	 * @param ip
	 * @param listenerPath
	 * @return
	 */
	private Boolean addSidWithListener(String sid, Connection conn, String ip) {
		Boolean flag = false;
		InputStream stdout = null;
		BufferedReader br = null;
		Session session = null;
		StringBuffer sb = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		StringBuffer sb3 = new StringBuffer();
		StringBuffer sidSb = new StringBuffer();
		Boolean isExist = false;
		String listenerPath = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_SCRIPT_ORACLE_DIR()+
				"/conf/listener.ora";
		//String listenerPath = "D:/eclipse64/workspace/pascloud-devops-parent/pascloud-webapps/src/main/webapp/static/resources/script/oracle/conf/conf/listener.ora";
		//String testpath = "/u01/app/oracle/product/11.2.0/dbhome_1";
		try {
			log.info("读取listener.ora文件");
			session = conn.openSession();
			session.execCommand("cat " + m_config.getORACLE_DBHOME()+"/network/admin/listener.ora");
			//session.execCommand("cat " + testpath+"/network/admin/listener.ora");
			
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			int index = 0;
			int listEndIndex = 0;
			int i = 0;
			sidSb.append("  (SID_DESC =").append("\n").append("  (GLOBAL_DBNAME = " + sid + ")").append("\n")
					.append("  (SID_NAME = " + sid + ")").append("\n").append("  )").append("\n");
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				sb.append(line).append("\n");
				if (line.contains("SID_LIST_LISTENER =")) {
					index = i;
				}
				if (line.contains("GLOBAL_DBNAME")) {
					String[] str = line.split("=");
					//System.out.println(str[1].toLowerCase().trim().toString());
					if (str.length == 2) {
						String name = str[1].toLowerCase().trim().toString();
						name = name.replace(")", "");
						if (sid.equals(name)) {
							System.out.println("sid已经存在");
							isExist = true;
						} 
					}
				}
				if(line.contains("  )") && index>0 && !isExist){
					listEndIndex = i;
				}
				i++;
			}
			if(!isExist && listEndIndex>0 && index>0){
				InputStream in = new ByteArrayInputStream(sb.toString().getBytes());
				BufferedReader br2 = new BufferedReader(new InputStreamReader(in));
				int j=0;
				while (true) {
					String line = br2.readLine();
					if (line == null) {
						break;
					}
					sb2.append(line).append("\n");
					if(line.contains("  )") && j==listEndIndex){
						sb2.append(sidSb.toString());
					}
					j++;
				}
				br2.close();
				in.close();
			}else if(index == 0 && !isExist){
				sb3.append("SID_LIST_LISTENER =").append("\n")
				   .append("(SID_LIST =").append("\n")
				   .append(sidSb.toString())
				   .append(")");
				sb2.append(sb.toString()).append(sb3.toString());
			}else if(isExist){
				sb2.append(sb.toString());
			}
			if(!isExist){
				if (!FileUtils.isFileExists(listenerPath)) {
					FileUtils.createOrExistsFile(listenerPath);
				}
				log.info("向listener.ora写入" + sid + "的配置内容");
				FileUtils.writeFileFromString(new File(listenerPath), sb2.toString(), false);
				flag = uploadTnsnameToServer(conn, listenerPath);
			}else{
				flag = true;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			log.info("发生异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			session.close();
		}

		return flag;
	}
	
	private Boolean delSidWithListener(String sid, Connection conn, String ip) {
		Boolean flag = false;
		InputStream stdout = null;
		BufferedReader br = null;
		Session session = null;
		StringBuffer sb = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		StringBuffer sb3 = new StringBuffer();
		StringBuffer sidSb = new StringBuffer();
		Boolean isExist = false;
		String listenerPath = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_SCRIPT_ORACLE_DIR()+
				"/conf/listener.ora";
		
		//String listenerPath ="D:/eclipse64/workspace/pascloud-devops-parent/pascloud-webapps/src/main/webapp/static/resources/script/oracle/conf/listener.ora";
		
		try {
			log.info("读取listener.ora文件");
			session = conn.openSession();
			session.execCommand("cat " + m_config.getORACLE_DBHOME()+"/network/admin/listener.ora");
			//session.execCommand("cat " + "/u01/app/oracle/product/11.2.0/dbhome_1/network/admin/listener.ora");
			
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			int index = 0;
			int listStartIndex = 0;
			int listEndIndex = 0;
			int i = 0;

			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}

				sb.append(line).append("\n");
				if (line.contains("SID_LIST_LISTENER =")) {
					index = i;
				}
				if (line.contains("GLOBAL_DBNAME")) {
					String[] str = line.split("=");
					//System.out.println(str[1].toLowerCase().trim().toString());
					if (str.length == 2) {
						String name = str[1].toLowerCase().trim().toString();
						name = name.replace(")", "");
						if (sid.equals(name)) {
							System.out.println("sid已经存在");
							isExist = true;
							listStartIndex = i;
						} 
					}
				}
				i++;
			}
			if(isExist && listStartIndex>0 && index>0){
				InputStream in = new ByteArrayInputStream(sb.toString().getBytes());
				BufferedReader br2 = new BufferedReader(new InputStreamReader(in));
				int j=0;
				while (true) {
					String line = br2.readLine();
					if (line == null) {
						break;
					}
					if(j>=listStartIndex-1 && j<=listStartIndex+2){
						
					}else{
						sb2.append(line).append("\n");
					}
					j++;
				}
				
				br2.close();
				in.close();
				if (!FileUtils.isFileExists(listenerPath)) {
					FileUtils.createOrExistsFile(listenerPath);
				}
				log.info("向listener.ora写入" + sid + "的配置内容");
				FileUtils.writeFileFromString(new File(listenerPath), sb2.toString(), false);
				flag = uploadTnsnameToServer(conn, listenerPath);
			}else{
				flag = true;
			}
			System.out.println(sb2.toString());
			
			
		} catch (Exception e) {
			log.error(e.getMessage());
			log.info("发生异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			session.close();
		}

		return flag;
	}

	public Boolean checkLsnrctlStatus(String ip, String sid) {
		Boolean flag = false;
		ServerVo vo = m_serverService.getByIP(ip);
		log.info("ip:" + ip + ",sid:" + sid);
		Connection conn = null;
		if (null != vo) {
			conn = getScpClientConn(vo.getIp(), vo.getUsername(), vo.getPassword());
			flag = checkLsnrctlStatus(conn, sid);
		}
		log.info("lsnrctl status:" + flag);
		return flag;
	}

	private Boolean checkLsnrctlStatus(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("检查" + sid + "监听是否启动");
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/lsnrctl_status.sh" + " " + sid +" "+m_config.getORACLE_DBHOME());
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				if (line.contains("Service \"" + sid + "\" has ")) {
					flag = true;
				}
				log.info(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("检查" + sid + "监听异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
			conn.close();
		}
		return flag;
	}
	
	public Integer checkSchemaIsExists(String sid,String url,String dbType,String username){
		Integer res = 0;
		
		String driverClass = DBUtils.getDirverClassName(dbType);
		DBUtils db = new DBUtils(driverClass, url, "pas", "pas");
		java.sql.Connection conn = null;
		
		try {
			QueryRunner qRunner = new QueryRunner();
			conn = db.getConnection();
			if (null != conn) {
				String sql = "select count(1) from all_users where username=upper(?)";
				Object[] params = new Object[] { username };
				Number num =  (Number)qRunner.query(conn,sql, new ScalarHandler(),params);
				if(null!=num){
					res = num.intValue();
				}
			} 
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		} finally {
			try {
				if(null!=conn){
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return res;
	}
	
	public List<DBUser> gerUserFromSid(String sid,String url,String dbType,String username,
			String password){
		List<DBUser> users = new ArrayList<>();
		String driverClass = DBUtils.getDirverClassName(dbType);
		DBUtils db = new DBUtils(driverClass, url, username, password);
		java.sql.Connection conn = null;
		
		try {
			QueryRunner qRunner = new QueryRunner();
			conn = db.getConnection();
			if (null != conn) {
				String sql = "select * from all_users where username like ?";
				String u = "PAS";
		        Object[] params = new Object[] { "%"+u+"%" };
				users = qRunner.query(conn,sql, new BeanListHandler<DBUser>(DBUser.class),params);
			} 
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		} finally {
			try {
				if(null!=conn){
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return users;
	}

	public Integer updateKhdxHy(String dbType, String url, String username, String password, String hyprefix) {

		String driverClass = DBUtils.getDirverClassName(dbType);
		DBUtils db = new DBUtils(driverClass, url, username, password);
		java.sql.Connection sourceConn = null;
		Integer result = 0;
		QueryRunner qRunner = new QueryRunner();
		log.info("driverClass=" + driverClass + ",url=" + url + ",username=" + username + ",password=" + password);
		// qRunner.insertBatch(conn, sql, ArrayHandle<KhdxHyVo>, params);
		try {
			sourceConn = db.getConnection();
			if (null != sourceConn) {
				String sql = "update khdx_hy set dlmc=replace(dlmc,substr(dlmc, 0, 2),?);";
				Object[] params = new Object[] { hyprefix };
				result = qRunner.update(sourceConn, sql, params);
			} else {

			}
			log.info("影响了" + result + "行");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		} finally {
			try {
				sourceConn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result;

	}

	private Boolean changeScriptOwn(Connection conn) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("更改目录的用户权限");
			session = conn.openSession();
			session.execCommand("chown -R oracle:oinstall "+Constants.LINUX_ORACLE_HOME_SCRIPT);
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
			}
			flag = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.info("更改/home/oracle/script目录的用户权限异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean changeScriptMod(Connection conn) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("更改目录的读写权限");
			session = conn.openSession();
			session.execCommand("chmod -R 755 "+Constants.LINUX_ORACLE_HOME_SCRIPT);
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
			}
			flag = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.info("更改"+Constants.LINUX_ORACLE_HOME_SCRIPT+"目录的读写权限异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	

	public Boolean checkDirIsExist(Connection conn,String dirPath){
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		StringBuffer sb = new StringBuffer();
		try {
			log.info("检查目录");
			session = conn.openSession();
			session.execCommand(" [ -s "+dirPath+" ] && echo \"true\" || echo \"false\"");
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				} else {
					log.info(line);
					sb.append(line);
				}
			}
			if("true".equals(sb.toString())){
				flag = true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("检查目录异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}
	
	public static void main(String[] args) throws Exception {

		DBServerService dbs = new DBServerService();
		
		Connection conn = null;
		Boolean flag = false;
		String oracle_dbhome = "/u01/app/oracle/product/11.2.0/dbhome_1";
		String sid = "cpas10";
		
		String tnsnamePath = "D:/eclipse64/workspace/pascloud-devops-parent/pascloud-webapps/src/main/webapp/static/resources/script/oracle/conf/conf/tnsnames.ora";
		try {

			StringBuffer sb = new StringBuffer();
			InputStream stdout = null;
			Session session = null;
			BufferedReader br = null;
			conn = dbs.getScpClientConn("121.33.38.199", "oracle", "oracle",10073);
			/*
			session = conn.openSession();
			session.execCommand(Constants.LINUX_ORACLE_HOME_SCRIPT+"/create_database.sh" + " " + sid+" "+oracle_dbhome+"/bin/dbca");
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					flag = true;
					break;
				} else {
					log.info(line);
				}
				sb.append(line);
			}
			*/
			
			//dbs.addSidWithTnsname(sid, conn, "192.168.53.83", tnsnamePath);
			//dbs.addSidWithListener(sid,conn,"192.168.53.83");
			
			//dbs.createTableSpaceWithSid(conn, sid);
			//dbs.grantWithSid(conn, sid);
			//dbs.restartListenerWithSid(conn, sid);
			
			//dbs.shutDownOracleWithSid(conn, sid);
			//dbs.deleteOracleWithSid(conn, sid);
			
			//String tnsnamePath ="D:/eclipse64/workspace/pascloud-devops-parent/pascloud-webapps/src/main/webapp/static/resources/script/oracle/conf/tnsnames.ora";
	
			//dbs.delSIDWithOratab(conn, sid, oratabfilePath);
			//dbs.delSidWithListener(sid, conn, "192.168.53.83");
			//dbs.delSidWithTnsname(sid, conn, "192.168.53.83", tnsnamePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		} finally {
			// session.close();
			
			
			if(null!=conn){
				conn.close();
			}
		}
	}
}
