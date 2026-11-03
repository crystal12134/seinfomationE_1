package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.config.DataSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SchemaKeyGuardTest {
 @Test void refusesWrongDatabaseKeyBeforeServingRequests() throws Exception {
  JdbcTemplate jdbc=mock(JdbcTemplate.class);var c=new DataSecurityConfig(jdbc);
  String data=Base64.getEncoder().encodeToString(new byte[32]);byte[] b=new byte[32];Arrays.fill(b,(byte)1);String lookup=Base64.getEncoder().encodeToString(b);
  ReflectionTestUtils.setField(c,"data",data);ReflectionTestUtils.setField(c,"lookup",lookup);
  when(jdbc.queryForObject(anyString(),eq(String.class))).thenReturn("2","wrong","wrong");
  assertThrows(IllegalStateException.class,()->c.run(null));
 }
}
