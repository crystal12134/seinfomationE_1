package com.seinfo.seinfomationA.util.token;

import com.seinfo.seinfomationA.component.main.mapper.LoginMapper;
import com.seinfo.seinfomationA.util.response.CoreWebApiUserDetail;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenUtils tokenUtils;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final LoginMapper loginMapper;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return tokenUtils.getTokenFromCookie(request) == null
                || uri.equals("/login/refreshToken")
                || uri.startsWith("/static/")
                || uri.startsWith("/sm/")
                || uri.startsWith("/.well-known");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = tokenUtils.getTokenFromCookie();
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2) 만료 여부 확인
            boolean isExpired = (boolean) tokenUtils.getClaimsByValue(TokenUtils.TOKEN_EXPIRATION_VALID);
            if (isExpired) {
                filterChain.doFilter(request, response);
                return;
            }

            String userEmail = String.valueOf(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID));
            Map<String, Object> user = loginMapper.selectUserByEmail(userEmail);
            if (user == null) {
                filterChain.doFilter(request, response);   // 사용자 자체를 못 찾으면 401 처리
                return;
            }
            user.put("userType", user.get("userRole"));

            // 1) Security 재인증
            CoreWebApiUserDetail coreWebApiUserDetail = new CoreWebApiUserDetail(user);
            AbstractAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    coreWebApiUserDetail, coreWebApiUserDetail.getPassword(), coreWebApiUserDetail.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            securityContextRepository.saveContext(securityContext, request, response);

        } catch (ExpiredJwtException | MalformedJwtException ex) {
            log.debug("JWT parsing error : {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}

