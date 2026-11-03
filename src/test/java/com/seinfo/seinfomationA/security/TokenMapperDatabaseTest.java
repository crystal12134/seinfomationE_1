package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.token.TokenMapper;
import com.seinfo.seinfomationA.util.mybatis.CamelMap;
import com.seinfo.seinfomationA.util.security.TokenFingerprint;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TokenMapperDatabaseTest {
 @Test void actualMapperStoresFingerprintsInExisting400CharacterColumns() throws Exception {
  var data=new DriverManagerDataSource("jdbc:h2:mem:tokens;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");var jdbc=new JdbcTemplate(data);
  jdbc.execute("CREATE TABLE zzz_token_access(token_user_id VARCHAR(40) PRIMARY KEY,token_key VARCHAR(400),user_ip_address VARCHAR(200),user_agent VARCHAR(200),reg_dt TIMESTAMP,expires_at TIMESTAMP)");
  jdbc.execute("CREATE TABLE zzz_token_denied(token_usr_id VARCHAR(40),token_key CHAR(64) PRIMARY KEY,token_denied_desc VARCHAR(400),reg_dt TIMESTAMP,expires_at TIMESTAMP)");
  var config=new org.apache.ibatis.session.Configuration();config.getTypeAliasRegistry().registerAlias("camelmap",CamelMap.class);
  var factory=new SqlSessionFactoryBean();factory.setDataSource(data);factory.setConfiguration(config);factory.setMapperLocations(new ClassPathResource("mybatis/tokken/token.xml"));
  try(var session=factory.getObject().openSession(true)) {
   var mapper=session.getMapper(TokenMapper.class);String raw="eyJ"+"long".repeat(400)+".payload.signature";
   Map<String,Object> row=new HashMap<>(Map.of("tokenUserId","alice","tokenKey",raw,"userIpAddress","127.0.0.1","userAgent","test","tokenDeniedDesc","logout"));
   mapper.insertTokenAccess(row);assertEquals(TokenFingerprint.of(raw),mapper.selectTokenAccess(row).get("tokenKey"));
   mapper.insertTokenBanned(row);assertEquals(1,mapper.countDeniedToken(TokenFingerprint.of(raw)));
   row.put("tokenKey",TokenFingerprint.of(raw));mapper.insertTokenBanned(row);assertEquals(1,mapper.countDeniedToken(TokenFingerprint.of(raw)));
  }
 }
}
