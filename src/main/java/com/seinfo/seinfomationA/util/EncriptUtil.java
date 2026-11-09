package com.seinfo.seinfomationA.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Versioned authenticated encryption. The lookup key is deliberately separate from the data key. */
public class EncriptUtil implements PasswordEncoder {
    private static final SecureRandom RANDOM=new SecureRandom();
    private static volatile byte[] dataKey;
    private static volatile byte[] lookupKey;
    public static synchronized void configure(String data,String lookup) {
        byte[] d=Base64.getDecoder().decode(data), l=Base64.getDecoder().decode(lookup);
        if(d.length!=32 || l.length!=32 || Arrays.equals(d,l)) throw new IllegalArgumentException("서로 다른 32바이트 DATA/LOOKUP 키가 필요합니다.");
        dataKey=d.clone();lookupKey=l.clone();
    }
    private static byte[] data() {byte[] key=dataKey;if(key==null)throw new IllegalStateException("데이터 암호화 키가 설정되지 않았습니다.");return key;}
    public static String lookupCompanyNo(String number) throws Exception {
        String canonical=number.replace("-","").trim();
        if(!canonical.matches("[0-9]{10}"))throw new IllegalArgumentException("사업자번호는 숫자 10자리여야 합니다.");
        byte[] key=lookupKey;if(key==null)throw new IllegalStateException("조회 키가 설정되지 않았습니다.");
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
        return "h2_"+HexFormat.of().formatHex(mac.doFinal(("company:v2:"+canonical).getBytes(StandardCharsets.UTF_8)));
    }
    public static String encryptCompanyNo(String number) throws Exception {
        String canonical=number.replace("-","").trim();lookupCompanyNo(canonical);
        return encrypt(canonical,"company");
    }
    public static String decryptCompanyNo(String ciphertext) throws Exception {return decrypt(ciphertext,"company");}
    public static String encryptAes256(String value) throws Exception {return encrypt(value,"param");}
    public static String decryptAes256(String value) throws Exception {return decrypt(value,"param");}
    private static String encrypt(String value,String purpose) throws Exception {
        byte[] nonce=new byte[12];RANDOM.nextBytes(nonce);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(data(),"AES"),new GCMParameterSpec(128,nonce));
        cipher.updateAAD(("seinfo:"+purpose+":v2").getBytes(StandardCharsets.UTF_8));
        byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] packet=Arrays.copyOf(nonce,nonce.length+encrypted.length);System.arraycopy(encrypted,0,packet,nonce.length,encrypted.length);
        return "v2:"+Base64.getUrlEncoder().withoutPadding().encodeToString(packet);
    }
    private static String decrypt(String value,String purpose) throws Exception {
        if(value==null || !value.startsWith("v2:") || value.length()>32768)throw new IllegalArgumentException("지원하지 않는 암호문입니다.");
        byte[] packet=Base64.getUrlDecoder().decode(value.substring(3));
        if(packet.length<28)throw new IllegalArgumentException("손상된 암호문입니다.");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(data(),"AES"),new GCMParameterSpec(128,Arrays.copyOf(packet,12)));
        cipher.updateAAD(("seinfo:"+purpose+":v2").getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(packet,12,packet.length-12),StandardCharsets.UTF_8);
    }
    @Override public String encode(CharSequence value){return BCrypt.hashpw(value.toString(),BCrypt.gensalt());}
    @Override public boolean matches(CharSequence value,String encoded){return BCrypt.checkpw(value.toString(),encoded);}
}
