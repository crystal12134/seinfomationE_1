package com.seinfo.seinfomationA.util.security;
import com.github.benmanes.caffeine.cache.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
@Component @Order(-90)
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private final Cache<String,AtomicInteger> counters=Caffeine.newBuilder().maximumSize(20000).expireAfterWrite(Duration.ofMinutes(1)).build();
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws IOException,ServletException {
        String path=req.getRequestURI();
        int limit=path.endsWith("authNumberChk")||path.endsWith("/passwordReset/complete")?10:path.endsWith("checkBizNumber")||path.endsWith("sendAuthMail")||path.endsWith("pwdFindProcess")||path.endsWith("unlockPwd")?5:path.endsWith("loginProcess")?30:path.startsWith("/upload")&&req.getMethod().equals("POST")?30:0;
        if(limit>0 && counters.get(req.getRemoteAddr()+":"+path,k->new AtomicInteger()).incrementAndGet()>limit) {
            res.setStatus(429);res.setHeader("Retry-After","60");res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"요청이 많습니다. 잠시 후 다시 시도해주세요.\"}");return;
        }
        chain.doFilter(req,res);
    }
}
