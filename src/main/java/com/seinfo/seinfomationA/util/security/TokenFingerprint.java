package com.seinfo.seinfomationA.util.security;
/** Existing token columns hold only a 64-character fingerprint, never a bearer credential. */
public final class TokenFingerprint {
    private TokenFingerprint() {}
    public static String of(String token) {
        if(token==null || token.isBlank()) throw new IllegalArgumentException("인증 토큰이 없습니다.");
        return token.matches("[0-9a-f]{64}")?token:PasswordVersion.of(token);
    }
}
