package com.seinfo.seinfomationA.util;

import org.apache.commons.codec.binary.Base64;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

public class EncriptUtil implements PasswordEncoder {

    private static final byte[] ivBytes = new byte[16]; // 16바이트 IV (전부 0)

    // 헥스 문자열 → 바이트 배열
    private static byte[] hexToBytes(String hex) {
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    // 바이트 배열 → 헥스 문자열
    private static String toHexString(byte[] bytes) {
        StringBuilder retString = new StringBuilder();
        for (byte b : bytes) {
            retString.append(Integer.toHexString(256 + (b & 0xFF)).substring(1));
        }
        return retString.toString();
    }

    // 키 길이를 지정된 길이(16, 24, 32)로 맞춰줌 (모자라면 0 패딩, 넘치면 자름)
    private static String fixKeyLength(String key, int requiredLength) {
        if (key.length() > requiredLength) {
            return key.substring(0, requiredLength);
        }
        return String.format("%-" + requiredLength + "s", key).replace(' ', '0');
    }

    // 고정 키 조합으로 Cipher 생성 (ECB 모드용)
    private static Cipher getCipher(String synchro1, String synchro2, String synchro3, String synchro4, boolean isEncryptMode)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        String keyStr = synchro1 + synchro1 + synchro2 + synchro3 + synchro4;
        byte[] rawKey = fixKeyLength(keyStr, 16).getBytes(StandardCharsets.UTF_8); // 16바이트 고정
        SecretKeySpec keySpec = new SecretKeySpec(rawKey, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(isEncryptMode ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec);
        return cipher;
    }

    // AES ECB 모드 암호화
    public static String encryptAes256(String str) throws Exception {
        Cipher cipher = getCipher("bb5", "1860", "17a74", "213f", true);
        return toHexString(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }

    // AES ECB 모드 복호화
    public static String decryptAes256(String text) throws Exception {
        Cipher cipher = getCipher("bb5", "1860", "17a74", "213f", false);
        return new String(cipher.doFinal(hexToBytes(text)), StandardCharsets.UTF_8);
    }

    // AES CBC 모드 암호화 (사용자 지정 키)
    public static String encryptAes256(String str, String key) throws Exception {
        String fixedKey = fixKeyLength(key, 16); // 16바이트 고정
        SecretKeySpec keySpec = new SecretKeySpec(fixedKey.getBytes(StandardCharsets.UTF_8), "AES");
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return Base64.encodeBase64String(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }

    // AES CBC 모드 복호화 (사용자 지정 키)
    public static String decryptAes256(String str, String key) throws Exception {
        String fixedKey = fixKeyLength(key, 16); // 16바이트 고정
        SecretKeySpec keySpec = new SecretKeySpec(fixedKey.getBytes(StandardCharsets.UTF_8), "AES");
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        return new String(cipher.doFinal(Base64.decodeBase64(str)), StandardCharsets.UTF_8);
    }

    // 객체 암호화 (SealedObject 이용)
    public SealedObject encryptObject(Serializable obj, byte[] key) throws Exception {
        SecretKeySpec sks = new SecretKeySpec(fixKeyLength(new String(key, StandardCharsets.UTF_8), 16).getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(ivBytes));
        return new SealedObject(obj, cipher);
    }

    // 객체 복호화 (SealedObject 이용)
    public Object decryptObject(SealedObject sealedObject, byte[] key) throws Exception {
        SecretKeySpec sks = new SecretKeySpec(fixKeyLength(new String(key, StandardCharsets.UTF_8), 16).getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, sks, new IvParameterSpec(ivBytes));
        return sealedObject.getObject(cipher);
    }

    // 비밀번호 암호화 (BCrypt)
    @Override
    public String encode(CharSequence rawPassword) {
        return BCrypt.hashpw(rawPassword.toString(), BCrypt.gensalt());
    }

    // 비밀번호 검증
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }
}
