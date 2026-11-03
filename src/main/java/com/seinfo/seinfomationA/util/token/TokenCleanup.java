package com.seinfo.seinfomationA.util.token;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
public class TokenCleanup {
    private final TokenMapper mapper;
    @Scheduled(initialDelay=300000,fixedDelay=300000)
    public void removeExpired(){mapper.deleteExpiredAccess();mapper.deleteExpiredDenied();}
}
