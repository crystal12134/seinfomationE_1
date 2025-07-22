package com.seinfo.protoA.util.security;

import com.seinfo.protoA.util.token.TokenUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthUtil {

    private final TokenUtils tokenUtils;

    /**
     * 권한 비교
     */
    public boolean isRoleInTokenAndAuth(String role) {
        return tokenHasRole(role) && hasRole(role);
    }

    /**
     * 스프링 권한 추출
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.replaceFirst("^ROLE_", ""))
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }

    /**
     * 토큰 권한 추출
     */
    public boolean tokenHasRole(String role) {
        String tokenRoles = "";
        try {
            tokenRoles = String.valueOf(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_TYPE));
        } catch (Exception e) {
            return false;
        }

        if (tokenRoles == null || tokenRoles.isBlank()) return false;
        return Arrays.stream(tokenRoles.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase(role));
    }
}