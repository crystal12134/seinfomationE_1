package com.seinfo.seinfomationA.config;
import com.seinfo.seinfomationA.util.token.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
@Configuration @RequiredArgsConstructor
public class WebSecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository repo=CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookiePath("/");repo.setCookieCustomizer(cookie->cookie.sameSite("Lax"));
        http.csrf(csrf->csrf.csrfTokenRepository(repo).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            .ignoringRequestMatchers("/anyone/member/regMember/AddressPopup"));
        http.formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .userDetailsService(userDetailsService)
            //.sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .sessionManagement(AbstractHttpConfigurer::disable)
            .securityContext(sc -> sc
                .requireExplicitSave(true)
                .securityContextRepository(
                    new org.springframework.security.web.context.NullSecurityContextRepository()))
            .requestCache(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter,UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
                    CsrfToken token=(CsrfToken)req.getAttribute(CsrfToken.class.getName());
                    if(token!=null)token.getToken();
                    chain.doFilter(req,res);
                }
            },CsrfFilter.class);
        http.authorizeHttpRequests(a->a
            .dispatcherTypeMatchers(DispatcherType.ERROR,DispatcherType.FORWARD).permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/member/api/worklog/**","/member/update","/member/save","/member/list","/member/scheduler").hasAnyRole("ADMIN","EMPLOYEE")
            .requestMatchers(HttpMethod.DELETE,"/member/{id}").hasAnyRole("ADMIN","EMPLOYEE")
            .requestMatchers("/anyone/noticeList/registNotice","/anyone/noticeList/deleteNotice","/board/registerFaq").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET,"/","/static/**","/favicon.ico","/error/**","/docAgree","/docPolicy","/anyone/**","/login/login","/login/passFind","/login/sessionInfo","/board/faqList","/board/freeBoardList","/board/freeBoardList/freeBoardDetail").permitAll()
            .requestMatchers("/anyone/member/**","/login/login/loginProcess","/login/login/unlockPwd","/login/passFind/**","/login/passwordReset","/login/passwordReset/complete","/login/tmpPassChange/changePwdProcess","/login/logout").permitAll()
            .requestMatchers("/board/freeBoardList/reloadFreeBoardList","/board/freeBoardList/reloadFreeBoardDetail","/board/freeBoardList/addCommentList","/anyone/noticeList/reloadNoticeList","/anyone/noticeList/getNoticeDetail","/api/getCodeList","/api/getEncryptParam","/tus/mfileInfo","/tus/mfileGroupInfo","/tus/mfilePermit","/private/filedownload").permitAll()
            .anyRequest().authenticated());
        http.exceptionHandling(e->e
            .authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");})
            .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"접근 권한 또는 요청 인증을 확인해주세요.\"}");}));
        http.headers(h->h.contentSecurityPolicy(c->c.policyDirectives("object-src 'none'; base-uri 'self'; frame-ancestors 'self'")));
        return http.build();
    }
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {return c.getAuthenticationManager();}
    // Prevent Spring Boot from invoking the same authentication filter a second time outside the security chain.
    @Bean public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter){
        var registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
    }
    @Bean public org.springframework.boot.web.servlet.FilterRegistrationBean<com.seinfo.seinfomationA.util.token.JwtRefreshFilter> refreshRegistration(com.seinfo.seinfomationA.util.token.JwtRefreshFilter filter){
        var registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
    }
}
