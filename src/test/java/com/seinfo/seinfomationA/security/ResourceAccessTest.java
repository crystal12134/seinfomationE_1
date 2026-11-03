package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.security.ResourceAccess;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
class ResourceAccessTest {
 JdbcTemplate db;ResourceAccess access;
 @BeforeEach void setup(){
  db=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));access=new ResourceAccess(db);
  db.execute("CREATE TABLE seinfo_vendor_manager(user_uid INT,user_email VARCHAR)");
  db.execute("CREATE TABLE seinfo_request(request_no VARCHAR,user_uid INT,mfile_group_id VARCHAR)");
  db.execute("CREATE TABLE seinfo_request_cont(request_no VARCHAR,mfile_group_id VARCHAR)");
  db.execute("CREATE TABLE seinfo_freeboard_posts(freeboard_no INT,user_id VARCHAR)");
  db.execute("CREATE TABLE seinfo_freeboard_comments(comment_no INT,user_id VARCHAR)");
  db.execute("CREATE TABLE seinfo_work(id INT,user_email VARCHAR,mfile_group_id VARCHAR)");
  db.execute("CREATE TABLE seinfo_mfile_group(mfile_group_id VARCHAR,reg_user_id VARCHAR)");
  db.execute("CREATE TABLE seinfo_notice(notice_no INT,mfile_group_id VARCHAR,notice_type VARCHAR)");
  db.update("INSERT INTO seinfo_vendor_manager VALUES(1,'alice'),(2,'bob')");
  db.update("INSERT INTO seinfo_request VALUES('REQ1',1,'grp-question')");
  db.update("INSERT INTO seinfo_freeboard_posts VALUES(1,'alice')");db.update("INSERT INTO seinfo_freeboard_comments VALUES(2,'alice')");
  db.update("INSERT INTO seinfo_work VALUES(3,'bob','grp-work')");
  db.update("INSERT INTO seinfo_mfile_group VALUES('grp-private','alice'),('grp-question','admin'),('grp-public','admin')");
  db.update("INSERT INTO seinfo_notice VALUES(1,'grp-public','N')");user("alice","USER");
 }
 void user(String name,String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(name,"",AuthorityUtils.createAuthorityList("ROLE_"+role)));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void ownerCanReadRequest(){assertDoesNotThrow(()->access.request("REQ1"));}
 @Test void otherUserCannotReadRequest(){user("bob","EMPLOYEE");assertThrows(AccessDeniedException.class,()->access.request("REQ1"));}
 @Test void otherUserCannotEditPostOrComment(){user("bob","USER");assertThrows(AccessDeniedException.class,()->access.post(1));assertThrows(AccessDeniedException.class,()->access.comment(2));}
 @Test void userCannotUseEmployeeWorkApis(){assertThrows(AccessDeniedException.class,()->access.work(3));}
 @Test void employeeWorkOwnerAllowed(){user("bob","EMPLOYEE");assertDoesNotThrow(()->access.work(3));}
 @Test void adminMayManageOtherUsers(){user("admin","ADMIN");assertDoesNotThrow(()->access.request("REQ1"));assertDoesNotThrow(()->access.post(1));assertDoesNotThrow(()->access.work(3));}
 @Test void publicNoticeOnlyAllowsRead(){SecurityContextHolder.clearContext();assertDoesNotThrow(()->access.fileGroup("grp-public",false,false));assertThrows(AccessDeniedException.class,()->access.fileGroup("grp-public",true,false));}
 @Test void privateFileNotPublic(){SecurityContextHolder.clearContext();assertThrows(AccessDeniedException.class,()->access.fileGroup("grp-private",false,false));}
 @Test void relatedRequestOwnerCanReadAdminAttachmentButNotDelete(){assertDoesNotThrow(()->access.fileGroup("grp-question",false,false));assertThrows(AccessDeniedException.class,()->access.fileGroup("grp-question",true,false));}
 @Test void groupIdCannotTraverse(){assertThrows(AccessDeniedException.class,()->access.fileGroup("../private",false,false));}
}
