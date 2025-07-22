package com.seinfo.protoA.util.token;

import io.jsonwebtoken.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenUtils {

    @Value("${jwt.token.secret}")
    private String secret;

    @Value("${jwt.token.expiration.default:0}")
    private Long expiration;

    @Value("${jwt.token.expiration.autoLogin:0}")
    private Long autoLoginExpiration;

    private final HttpServletRequest req;

    public static final String TOKEN_KEY_HEADER = "X-AUTH-TOKEN";
    public static final String TOKEN_USER_ID = "USER_ID";
    public static final String TOKEN_USER_NM = "USER_NM";
    public static final String TOKEN_EMP_NO = "EMP_NO";
    public static final String TOKEN_USER_EMP_NO = "USER_EMP_NO";
    public static final String TOKEN_COMPANY_ORIGIN_CD = "COMPANY_ORIGIN_CD";
    public static final String TOKEN_DEPT_ID = "DEPT_ID";
    public static final String TOKEN_DEPT_NM = "DEPT_NM";
    public static final String TOKEN_USER_TYPE = "USER_TYPE";
    public static final String TOKEN_EXPIRETIME = "EXPIRETIME";
    public static final String TOKEN_NAME = "TOKEN";
    public static final String TOKEN_AUTHORITIES = "AUTHORITIES";
    public static final String TOKEN_EXPIRATION = "EXPIRATION";
    public static final String TOKEN_EXPIRATION_AUTOLOGIN = "EXPIRATION";
    public static final String TOKEN_EXPIRATION_VALID = "EXPIRATION_VALID";
    public static final String TOKEN_REFRESH_DEFAULT = "TOKEN_REFRESH_DEFAULT";
    public static final String TOKEN_REFRESH_TYPE1 = "TOKEN_REFRESH_TYPE1";

    private SecretKey getSecretKey(String base64Secret) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
    }

    private Claims getClaimsFromTokenInternal(String token, String base64Secret) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey(base64Secret))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        } catch (JwtException ex) {
            return null;
        }
    }

    public String getTokenFromCookie() {
        return getTokenFromCookie(this.req);
    }

    public String getTokenFromCookie(HttpServletRequest httpReq) {
        if (httpReq.getCookies() == null) return null;

        return Arrays.stream(httpReq.getCookies())
                .filter(cookie -> "X-AUTH-TOKEN".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private Claims getClaimsFromToken(String token) {
        return getClaimsFromTokenInternal(token, this.secret);
    }

    private Claims getClaimsFromToken(String token, String secret) {
        return getClaimsFromTokenInternal(token, secret);
    }

    private String generateClaims(Map<String, Object> claims, Long expirationTime) {
        return generateToken(claims, this.secret, expirationTime);
    }

    public String generateToken(Map<String, Object> claims) {
        return generateToken(claims, this.secret, this.expiration);
    }

    public String generateToken(Map<String, Object> claims, String tmpSecret) {
        long expireTime = Long.parseLong(claims.getOrDefault("EXPIRETIME", "0").toString());
        claims.remove("EXPIRETIME");
        return generateToken(claims, tmpSecret, expireTime);
    }

    private String generateToken(Map<String, Object> claims, String secret, long expireTime) {
        SecretKey key = getSecretKey(secret);
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expireTime * 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Object getClaimsByValue(String token, String value) {
        return extractClaim(getClaimsFromToken(token), value);
    }

    public Object getClaimsByValue(String token, String value, String secret) {
        return extractClaim(getClaimsFromToken(token, secret), value);
    }

    public Object getClaimsByValue(String value) {
        String token = getTokenFromCookie();
        if (token == null) token = this.req.getHeader("X-AUTH-TOKEN");
        if (token == null) return null;
        return extractClaim(getClaimsFromToken(token), value);
    }

    private Object extractClaim(Claims claims, String value) {
        return switch (value) {
            case "EXPIRATION" -> claims.getExpiration();
            case "EXPIRATION_VALID" -> claims.getExpiration().before(new Date(System.currentTimeMillis()));
            default -> claims.get(value);
        };
    }

    public String refreshToken(String type) {
        String token = getTokenFromCookie();
        if (token == null) token = this.req.getHeader("X-AUTH-TOKEN");

        Claims claims = getClaimsFromToken(token);
        long expire = "TOKEN_REFRESH_TYPE1".equals(type) ? this.autoLoginExpiration : this.expiration;
        return generateClaims(claims, expire);
    }

    public boolean validateToken(String token, String username) {
        return validateToken(token, username, this.secret);
    }

    public boolean validateToken(String token, String username, String secret) {
        if (username == null || token == null) {
            log.info(">> Token Validate Failed - Token present: {}, Username present: {}", token != null, username != null);
            return false;
        }

        try {
            String tokenUserId = (String) getClaimsByValue(token, "USER_ID", secret);
            if (!username.equals(tokenUserId)) {
                log.info(">> Token id: {} / Login id: {} mismatch", tokenUserId, username);
                return false;
            }

            Boolean isExpired = (Boolean) getClaimsByValue(token, "EXPIRATION_VALID", secret);
            if (Boolean.TRUE.equals(isExpired)) {
                log.info(">> Token expired for id: {}", username);
                return false;
            }

            getClaimsFromToken(token, secret); // signature check
            log.info(">> Token Validate Successful UserId: {}", username);
            return true;

        } catch (Exception e) {
            log.error(">> Token Validation Error for user: {}, Error: {}", username, e.getMessage());
            return false;
        }
    }
}