package com.seinfo.seinfomationA.security;

import com.seinfo.seinfomationA.component.main.service.impl.PasswordResetService;
import com.seinfo.seinfomationA.util.api.ApiService;
import com.seinfo.seinfomationA.util.security.PasswordVersion;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetSecurityTest {
 JdbcTemplate jdbc;TransactionTemplate tx;PasswordResetService service;ApiService mail;String original;
 @BeforeEach void setup(){
  var ds=new DriverManagerDataSource("jdbc:h2:mem:reset"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));mail=mock(ApiService.class);service=new PasswordResetService(jdbc,mail);ReflectionTestUtils.setField(service,"publicUrl","https://demo.example.invalid");
  jdbc.execute("CREATE TABLE seinfo_vendor_manager(user_uid INT PRIMARY KEY,login_id VARCHAR(40),user_email VARCHAR(80),user_pwd VARCHAR(100),bl_yn VARCHAR(1),user_pwd_init_yn VARCHAR(1),fail_cnt INT,fail_dt TIMESTAMP,user_pwd_ch_d DATE,user_login_d DATE,mod_dt TIMESTAMP,mod_user_id VARCHAR(80))");
  jdbc.execute("CREATE TABLE seinfo_password_reset(reset_key CHAR(64) PRIMARY KEY,user_uid INT UNIQUE,password_version CHAR(64),created_at TIMESTAMP,expires_at TIMESTAMP)");
  jdbc.execute("CREATE TABLE zzz_token_access(token_user_id VARCHAR(80) PRIMARY KEY,token_key VARCHAR(64))");
  original=BCrypt.hashpw("Original9!pass",BCrypt.gensalt());jdbc.update("INSERT INTO seinfo_vendor_manager(user_uid,login_id,user_email,user_pwd,bl_yn,user_pwd_init_yn,fail_cnt) VALUES(1,'member01','owner@example.invalid',?,'N','N',4)",original);jdbc.update("INSERT INTO zzz_token_access VALUES('owner@example.invalid','active-login')");
 }
 String request()throws Exception{
  tx.executeWithoutResult(s->service.request("member01"));
  var capture=org.mockito.ArgumentCaptor.forClass(Map.class);verify(mail).sendMail(capture.capture(),eq("seinfo/mail/mailPasswordReset"));
  var data=capture.getValue();assertEquals("owner@example.invalid",data.get("userEmail"));String url=data.get("resetUrl").toString();assertTrue(url.startsWith("https://demo.example.invalid/login/passwordReset#token="));return url.substring(url.indexOf("#token=")+7);
 }
 void complete(String token,String password){tx.executeWithoutResult(s->service.complete(token,password));}
 String password(){return jdbc.queryForObject("SELECT user_pwd FROM seinfo_vendor_manager WHERE user_uid=1",String.class);}
 @Test void requestOnlySendsLinkAndStoresHashWithoutChangingPasswordOrLogin()throws Exception{
  String token=request();assertEquals(original,password());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM zzz_token_access",Integer.class));
  assertEquals(43,token.length());assertEquals(PasswordVersion.of(token),jdbc.queryForObject("SELECT reset_key FROM seinfo_password_reset",String.class));assertNotEquals(token,jdbc.queryForObject("SELECT reset_key FROM seinfo_password_reset",String.class));
 }
 @Test void validLinkChangesPasswordRevokesLoginAndCannotBeReused()throws Exception{
  String token=request();complete(token,"Changed9!pass");assertTrue(BCrypt.checkpw("Changed9!pass",password()));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM zzz_token_access",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM seinfo_password_reset",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT fail_cnt FROM seinfo_vendor_manager",Integer.class));
  assertThrows(IllegalArgumentException.class,()->complete(token,"Another9!pass"));assertTrue(BCrypt.checkpw("Changed9!pass",password()));
 }
 @Test void expiredOrForgedLinksCannotChangePassword()throws Exception{
  String token=request();assertThrows(IllegalArgumentException.class,()->complete("A".repeat(43),"Changed9!pass"));
  jdbc.update("UPDATE seinfo_password_reset SET expires_at=?",Timestamp.from(Instant.now().minusSeconds(1)));assertThrows(IllegalArgumentException.class,()->complete(token,"Changed9!pass"));assertEquals(original,password());
 }
 @Test void passwordChangedElsewhereInvalidatesOutstandingLink()throws Exception{
  String token=request();String other=BCrypt.hashpw("Elsewhere9!pass",BCrypt.gensalt());jdbc.update("UPDATE seinfo_vendor_manager SET user_pwd=?",other);assertThrows(IllegalArgumentException.class,()->complete(token,"Changed9!pass"));assertEquals(other,password());
 }
 @Test void resendIsLimitedAndUnknownOrBlockedAccountsDoNotSendMail()throws Exception{
  request();tx.executeWithoutResult(s->service.request("member01"));verify(mail,times(1)).sendMail(anyMap(),anyString());
  tx.executeWithoutResult(s->service.request("unknown01"));jdbc.update("UPDATE seinfo_vendor_manager SET bl_yn='Y'");tx.executeWithoutResult(s->service.request("member01"));verify(mail,times(1)).sendMail(anyMap(),anyString());assertEquals(original,password());
 }
 @Test void onlyOneConcurrentRedemptionSucceeds()throws Exception{
  String token=request();var gate=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
  try{
   Callable<Boolean> redeem=()->{gate.await();try{complete(token,"Changed9!pass");return true;}catch(IllegalArgumentException e){return false;}};
   Future<Boolean> a=executor.submit(redeem),b=executor.submit(redeem);gate.countDown();int successes=(a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0);assertEquals(1,successes);
  }finally{executor.shutdownNow();}
 }
 @Test void databaseFailureRollsBackPasswordChangeAndTokenConsumption()throws Exception{
  String token=request();jdbc.execute("DROP TABLE zzz_token_access");assertThrows(org.springframework.dao.DataAccessException.class,()->complete(token,"Changed9!pass"));assertEquals(original,password());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM seinfo_password_reset",Integer.class));
 }
 @Test void invalidPasswordDoesNotConsumeLinkAndNewRequestInvalidatesOldLink()throws Exception{
  String token=request();assertThrows(IllegalArgumentException.class,()->complete(token,"short"));assertEquals(original,password());
  jdbc.update("UPDATE seinfo_password_reset SET created_at=?",Timestamp.from(Instant.now().minusSeconds(61)));tx.executeWithoutResult(s->service.request("member01"));assertThrows(IllegalArgumentException.class,()->complete(token,"Changed9!pass"));assertEquals(original,password());
 }
}
