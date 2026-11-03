package com.seinfo.seinfomationA.util.security;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
public final class PasswordVersion {
    private PasswordVersion() {}
    public static String of(String encodedPassword) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encodedPassword.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
