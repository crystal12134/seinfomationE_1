package com.seinfo.seinfomationA.security;

import com.seinfo.seinfomationA.component.board.service.impl.*;
import com.seinfo.seinfomationA.component.board.mapper.*;
import com.seinfo.seinfomationA.component.board.Model.FreeBoardModel;
import com.seinfo.seinfomationA.component.main.service.impl.LoginServiceImpl;
import com.seinfo.seinfomationA.component.main.mapper.LoginMapper;
import com.seinfo.seinfomationA.component.file.mapper.TusMapper;
import com.seinfo.seinfomationA.component.file.service.TusService;
import com.seinfo.seinfomationA.util.*;
import com.seinfo.seinfomationA.util.api.ApiService;
import com.seinfo.seinfomationA.util.response.*;
import com.seinfo.seinfomationA.util.security.*;
import com.seinfo.seinfomationA.util.token.*;
import com.seinfo.seinfomationA.util.mybatis.CamelMap;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecurityFix4RegressionTest {
 DriverManagerDataSource ds;JdbcTemplate jdbc;MockHttpServletRequest req;TokenUtils tokens;ResponseEntityUtil responses;
 @BeforeEach void setup(){
  ds=new DriverManagerDataSource("jdbc:h2:mem:review"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);req=new MockHttpServletRequest();responses=new ResponseEntityUtil(req);tokens=mock(TokenUtils.class);when(tokens.getClaimsByValue("USER_ID")).thenReturn("alice@example.invalid");
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice@example.invalid","",AuthorityUtils.createAuthorityList("ROLE_USER")));
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 org.apache.ibatis.session.SqlSession session(String xml)throws Exception{
  var config=new org.apache.ibatis.session.Configuration();config.getTypeAliasRegistry().registerAlias("camelmap",CamelMap.class);var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);factory.setMapperLocations(new ClassPathResource(xml));return factory.getObject().openSession(true);
 }
 void boards(){jdbc.execute("CREATE TABLE seinfo_freeboard_posts(freeboard_no VARCHAR(40) PRIMARY KEY,user_id VARCHAR(80),freeboard_title VARCHAR(100),freeboard_content VARCHAR(1000),mod_dt TIMESTAMP,mod_user_id VARCHAR(80))");jdbc.execute("CREATE TABLE seinfo_freeboard_comments(comment_no VARCHAR(40) PRIMARY KEY,parent_no VARCHAR(40),freeboard_no VARCHAR(40),user_id VARCHAR(80))");jdbc.update("INSERT INTO seinfo_freeboard_posts VALUES('own','alice@example.invalid','own','own',NOW(),NULL),('victim','bob@example.invalid','original','original',NOW(),NULL)");jdbc.update("INSERT INTO seinfo_freeboard_comments VALUES('victim-comment',NULL,'victim','bob@example.invalid'),('own-comment',NULL,'own','alice@example.invalid')");}
 FreeBoardServiceImpl boardService(FreeBoardMapper mapper){return new FreeBoardServiceImpl(new ResourceAccess(jdbc),tokens,mock(ComUtil.class),mapper,mock(TusMapper.class),mock(TusService.class),mock(AuthUtil.class),req.getSession(),req,responses);}
 @Test void bothUpdateRoutesRejectOtherOwnerAndAllowOwnerAndAdmin()throws Exception{
  boards();try(var sql=session("mybatis/board/freeboard.xml")){
   var service=boardService(sql.getMapper(FreeBoardMapper.class));var model=new FreeBoardModel();model.setFreeBoardNo("victim");model.setModifyYn("Y");model.setFreeBoardTitle("changed-by-alice");model.setFreeBoardContent("changed");
   assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.updateFreeBoard(model));
   assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.registFreeBoard(model));
   assertEquals("original",jdbc.queryForObject("SELECT freeboard_title FROM seinfo_freeboard_posts WHERE freeboard_no='victim'",String.class));
   model.setFreeBoardNo("own");assertEquals(200,service.registFreeBoard(model).getStatusCode().value());
   assertEquals("changed-by-alice",jdbc.queryForObject("SELECT freeboard_title FROM seinfo_freeboard_posts WHERE freeboard_no='own'",String.class));
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@example.invalid","",AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
   model.setFreeBoardNo("victim");service.updateFreeBoard(model);
   assertEquals("changed-by-alice",jdbc.queryForObject("SELECT freeboard_title FROM seinfo_freeboard_posts WHERE freeboard_no='victim'",String.class));
  }
 }
 @Test void deletingOwnedPostIgnoresUnrelatedCommentAndPreservesVictim()throws Exception{
  boards();try(var sql=session("mybatis/board/freeboard.xml")){
   boardService(sql.getMapper(FreeBoardMapper.class)).deletePostWithComments(new HashMap<>(Map.of("freeboardNo","own","commentNo","victim-comment")));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM seinfo_freeboard_comments WHERE comment_no='victim-comment'",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM seinfo_freeboard_posts WHERE freeboard_no='victim'",Integer.class));
   assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM seinfo_freeboard_comments WHERE freeboard_no='own'",Integer.class));
  }
 }
 public static String dateFormat(java.sql.Timestamp value,String pattern){return value==null?null:value.toLocalDateTime().toLocalDate().toString();}
 @Test void browserSuppliedAdminValuesAreIgnoredButRealAdminCanListAll()throws Exception{
  jdbc.execute("CREATE ALIAS DATE_FORMAT FOR 'com.seinfo.seinfomationA.security.SecurityFix4RegressionTest.dateFormat'");
  jdbc.execute("CREATE TABLE seinfo_vendor_manager(user_uid INT PRIMARY KEY,user_email VARCHAR(80),user_pwd_init_yn VARCHAR(1),bl_yn VARCHAR(1),user_role VARCHAR(10),login_id VARCHAR(40))");
  jdbc.execute("CREATE TABLE seinfo_notice(notice_no VARCHAR(40),notice_type VARCHAR(1),notice_title VARCHAR(100),notice_contents VARCHAR(1000),notice_d TIMESTAMP,user_uid INT,answer_contents VARCHAR(1000),answer_d TIMESTAMP,answer_user_id VARCHAR(80))");
  jdbc.update("INSERT INTO seinfo_vendor_manager VALUES(1,'alice@example.invalid','N','N','USER','alice'),(2,'bob@example.invalid','N','N','USER','bob')");
  jdbc.update("INSERT INTO seinfo_notice(notice_no,notice_type,notice_title,notice_contents,notice_d,user_uid) VALUES('Q_1','Q','alice own','own',NOW(),1),('Q_2','Q','bob private title','private',NOW(),2)");
  try(var sql=session("mybatis/board/qna.xml")){
   var service=new QnaServiceImpl(tokens,responses,sql.getMapper(QnaMapper.class),new AuthUtil(tokens));
   var ordinary=(CoreWebApiResponseModel)service.reloadQnaPage(new HashMap<>(Map.of("userType","USER","page",1))).getBody();
   var forged=(CoreWebApiResponseModel)service.reloadQnaPage(new HashMap<>(Map.of("userType","ADMIN","page",1,"isAdmin",true,"userEmail","bob@example.invalid","adminYn","Y"))).getBody();
   assertEquals(1,((List<?>)((Map<?,?>)ordinary.getData()).get("qnaList")).size());assertEquals(1,((List<?>)((Map<?,?>)forged.getData()).get("qnaList")).size());
   when(tokens.getClaimsByValue("USER_TYPE")).thenReturn("ADMIN");
   var stale=(CoreWebApiResponseModel)service.reloadQnaPage(new HashMap<>(Map.of("userType","ADMIN","page",1))).getBody();
   assertEquals(1,((List<?>)((Map<?,?>)stale.getData()).get("qnaList")).size());
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@example.invalid","",AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
   var memberAdmin=(CoreWebApiResponseModel)service.reloadQnaPage(Map.of("page",1)).getBody();
   assertEquals(0,((List<?>)((Map<?,?>)memberAdmin.getData()).get("qnaList")).size());
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice@example.invalid","",AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
   var ownAdmin=(CoreWebApiResponseModel)service.reloadQnaPage(Map.of("page",1)).getBody();
   assertEquals(1,((List<?>)((Map<?,?>)ownAdmin.getData()).get("qnaList")).size());
   var admin=(CoreWebApiResponseModel)service.reloadQnaPage(new HashMap<>(Map.of("userType","USER","page",1,"qnaMode","admin"))).getBody();
   assertEquals(2,((List<?>)((Map<?,?>)admin.getData()).get("qnaList")).size());
   SecurityContextHolder.clearContext();assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.reloadQnaPage(Map.of("page",1)));
  }
 }
 @Test void recoveryRequestDoesNotInvokeLegacyPasswordChange()throws Exception{
  SecurityContextHolder.clearContext();var login=mock(LoginMapper.class);var api=mock(ApiService.class);var resets=mock(com.seinfo.seinfomationA.component.main.service.impl.PasswordResetService.class);
  var service=new LoginServiceImpl(new MockHttpServletResponse(),req,mock(AuthenticationManager.class),login,mock(TokenMapper.class),responses,tokens,api,resets);
  assertEquals(200,service.pwdFindProcess(new HashMap<>(Map.of("id","bob"))).getStatusCode().value());
  verify(resets).request("bob");verify(login,never()).updateUsrTempPwd(anyMap());verifyNoInteractions(api);
 }
}
