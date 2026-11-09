package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.config.DataSecurityConfig;
import com.seinfo.seinfomationA.util.EncriptUtil;
import com.seinfo.seinfomationA.util.token.*;
import com.seinfo.seinfomationA.component.member.mapper.MemberMapper;
import com.seinfo.seinfomationA.component.member.service.MemberService;
import com.seinfo.seinfomationA.util.response.CoreWebApiResponseModel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver","spring.sql.init.mode=never","tus.server.data.directory=${java.io.tmpdir}/seinfo-v2-integration-upload"})
@EnabledIfEnvironmentVariable(named="SEINFO_RUN_MYSQL_TESTS",matches="true")
@Transactional
class MysqlMigrationIntegrationTest {
 @Autowired DataSecurityConfig config;
 @Autowired JdbcTemplate jdbc;
 @Autowired MemberService members;
 @Autowired MemberMapper memberMapper;
 @Autowired TokenMapper tokens;
 @Autowired TokenUtils tokenUtils;
 @Autowired org.springframework.mock.web.MockHttpServletRequest request;
 @Autowired org.mybatis.spring.SqlSessionTemplate sessions;
 @BeforeEach void setup(){config.configure();}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void migratedMemberInfoDecryptsAndMatchesCompanyLookup() throws Exception {
  String email=jdbc.queryForObject("SELECT user_email FROM seinfo_vendor_manager ORDER BY user_uid LIMIT 1",String.class);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,"",AuthorityUtils.createAuthorityList("ROLE_USER")));
  request.setCookies(new jakarta.servlet.http.Cookie("X-AUTH-TOKEN","integration-request"));
  var response=(CoreWebApiResponseModel)members.getMemberInfo().getBody();assertEquals("00",response.getProcess_code());
  Map<?,?> data=(Map<?,?>)response.getData();assertFalse(data.containsKey("companyNoEnc"));
  String expected=jdbc.queryForObject("SELECT company_no FROM seinfo_vendor_manager WHERE user_email=?",String.class,email);
  assertTrue(expected.equals(EncriptUtil.lookupCompanyNo(String.valueOf(data.get("companyNo")))));
 }
 @Test void actualVendorMapperStoresV2CipherAndDoesNotOverwriteExistingProfile() throws Exception {
  String number="9999999999";String lookup=EncriptUtil.lookupCompanyNo(number);
  Map<String,Object> row=new HashMap<>();row.put("companyNo",lookup);row.put("companyNoEnc",EncriptUtil.encryptCompanyNo(number));
  for(String key:List.of("ceoNm","companyNm","companyAddress","companyAddressDetail","ceoPhone","ceoEmail","companyUrl","companyInfo","regUserId","modUserId"))row.put(key,"test");row.put("companyD","2020-01-01");
  memberMapper.insertVendor(row);String encrypted=jdbc.queryForObject("SELECT company_no_enc FROM seinfo_vendor WHERE company_no=?",String.class,lookup);assertTrue(number.equals(EncriptUtil.decryptCompanyNo(encrypted)));
  row.put("companyNm","must-not-overwrite");memberMapper.insertVendor(row);assertEquals("test",jdbc.queryForObject("SELECT company_nm FROM seinfo_vendor WHERE company_no=?",String.class,lookup));
 }
 @Test void actualTokenMapperAndValidationHonorRevocationAndExpiry() throws Exception {
  String email="integration@example.invalid";String token=tokenUtils.generateToken(new HashMap<>(Map.of("USER_ID",email)));
  Map<String,Object> row=new HashMap<>(Map.of("tokenUserId",email,"tokenKey",token,"userIpAddress","127.0.0.1","userAgent","test","tokenDeniedDesc","test"));
  tokens.insertTokenAccess(row);assertTrue(tokenUtils.validateToken(token,email));
  tokens.insertTokenBanned(row);assertFalse(tokenUtils.validateToken(token,email));
  jdbc.update("UPDATE zzz_token_access SET expires_at=TIMESTAMPADD(MINUTE,-1,NOW()) WHERE token_user_id=?",email);
  sessions.clearCache();
  assertNull(tokens.selectTokenAccess(row));assertTrue(tokens.deleteExpiredAccess()>=1);
 }
}
