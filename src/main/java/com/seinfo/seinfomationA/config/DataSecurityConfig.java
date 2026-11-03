package com.seinfo.seinfomationA.config;
import com.seinfo.seinfomationA.util.EncriptUtil;
import com.seinfo.seinfomationA.util.security.PasswordVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
@Configuration @DependsOnDatabaseInitialization
public class DataSecurityConfig {
    @Value("${seinfo.crypto.data-key}") private String data;
    @Value("${seinfo.crypto.lookup-key}") private String lookup;
    private final JdbcTemplate jdbc;
    public DataSecurityConfig(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @PostConstruct public void configure(){EncriptUtil.configure(data,lookup);run(null);}
    private static String fingerprint(String value){
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(value)));}
        catch(Exception e){throw new IllegalStateException("암호화 키 설정 오류입니다.",e);}
    }
    public void run(ApplicationArguments arguments) {
        try{
            String version=jdbc.queryForObject("SELECT meta_value FROM seinfo_security_meta WHERE meta_key='schema_version'",String.class);
            String dataId=jdbc.queryForObject("SELECT meta_value FROM seinfo_security_meta WHERE meta_key='data_key_id'",String.class);
            String lookupId=jdbc.queryForObject("SELECT meta_value FROM seinfo_security_meta WHERE meta_key='lookup_key_id'",String.class);
            if(!"2".equals(version)||!fingerprint(data).equals(dataId)||!fingerprint(lookup).equals(lookupId))throw new IllegalStateException("DB 버전 또는 암호화 키가 일치하지 않습니다.");
        }catch(Exception e){throw new IllegalStateException("보안 v2 DB를 먼저 이관하고 해당 DB와 함께 받은 DATA/LOOKUP 키를 설정해주세요.",e);}
    }
}
