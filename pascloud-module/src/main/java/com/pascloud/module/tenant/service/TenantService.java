package com.pascloud.module.tenant.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.pascloud.module.mycat.service.MycatService;
import com.pascloud.utils.DBUtils;
import com.pascloud.vo.mycat.DataSourceVo;
import com.pascloud.vo.tenant.KhdxHyVo;

@Service
public class TenantService {
	
	private static Logger log = LoggerFactory.getLogger(TenantService.class);
	
	
	 public Integer updateKhdxHy(Connection conn,String en,String cn, String name){
			
			Integer result = 0;
			Integer result2 = 0;
			Integer result3 = 0;
			Integer result4 = 0;
			QueryRunner qRunner = new QueryRunner();  
			try {
				if(null!=conn){
					String sql = "update khdx_hy set dlmc=replace(dlmc,substr(dlmc, 0, 2),?)";
					Object [] params = new Object[]{en};
					result = qRunner.update(conn, sql, params);
					
					String xmmc = cn+"农商行绩效系统";
					String sql2 = "update xtb_xtcs set CSZ=? where csmc='SYS_XMMC'";
					Object [] params2 = new Object[]{xmmc};
					result2 = qRunner.update(conn, sql2, params2);
					String dlmc = en+"admin";
					String old_dlmc=en+"as11";
					
					String sql3 = "update khdx_hy set dlmc=?,hymc=? where khdxdh=? ";
					Object [] params3 = new Object[]{dlmc,dlmc,1};
					result3 = qRunner.update(conn, sql3, params3);
					
					String sql4 = "update xtb_jsgnqx set fhdh=? ";
					Object [] params4 = new Object[]{name};
					result4 = qRunner.update(conn, sql4, params4);
					
				}else{
					
				}
				log.info("影响了"+result+"行");
				log.info("影响了"+result2+"行");
				log.info("影响了"+result3+"行");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}finally{
			}
			
			return result;
			
		}
	
	@SuppressWarnings("unchecked")
	public List<KhdxHyVo> getAllHy(Connection conn){
		List<KhdxHyVo> hys = new ArrayList<>();
		try {
			String sql = "select * from khdx_hy";
			QueryRunner qRunner = new QueryRunner();  
			hys =  qRunner.query(conn,sql, new BeanListHandler<KhdxHyVo>(KhdxHyVo.class));
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			log.info("查询表所有数据--失败--");
			log.error(e.getMessage());
			e.printStackTrace();
		}finally{
		}
		return hys;
	}
	
	public Integer deleteHyWithPublicDB(Connection conn,String id){
		Integer result = 0;
		try {
			String sql = "delete from khdx_hy where fhdh=?";
			QueryRunner qRunner = new QueryRunner();  
			result =  qRunner.update(conn,sql,id);
			//Gson g = new Gson();
			//System.out.println(g.toJson(hys));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
			//e.printStackTrace();
		}finally{
			
		}
		return result;
	}
	
	public Integer addHyWithPublicDB(Connection conn,String id,List<KhdxHyVo> lists){
		
		Integer result = 0;
		QueryRunner qRunner = new QueryRunner();  
		
		//qRunner.insertBatch(conn, sql, ArrayHandle<KhdxHyVo>, params);
		try {
			Object[][] params = new Object[lists.size()][];
			for(int i=0;i<lists.size();i++){
				KhdxHyVo hy = lists.get(i);
				params[i] = new Object[]{hy.getKhdxdh(),hy.getHydh(),hy.getHymc(),
						hy.getXl(),hy.getLxdh(),hy.getSfz(),hy.getYxrybz(),hy.getXnhybz(),
						hy.getDlmc(),hy.getDlmm(),hy.getAqjb(),hy.getZxzt(),hy.getScdl(),hy.getZpxx(),
						hy.getCzybh(),hy.getZxrq(),hy.getCsrq(),hy.getGzrq(),hy.getRhrq(),
						hy.getFgbz(),hy.getPxbz(),id};
			}
			
			String sql = "insert into khdx_hy(khdxdh,hydh,hymc,xl,lxdh,sfz,yxrybz,xnhybz,dlmc,dlmm,"
					+ "aqjb,zxzt,scdl,zpxx,czybh,zxrq,csrq,gzrq,rhrq,fgbz,pxbz,fhdh)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			qRunner.batch(conn, sql, params);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			log.error(e.getMessage());
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		return result;
		
	}

}
