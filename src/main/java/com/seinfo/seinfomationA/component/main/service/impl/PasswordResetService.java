package com.seinfo.seinfomationA.component.main.service.impl;

import com.seinfo.seinfomationA.util.LoginIdPolicy;
import com.seinfo.seinfomationA.util.api.ApiService;
import com.seinfo.seinfomationA.util.security.PasswordVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/** Mailbox proof is required before changing any password or login state. */
@Service @RequiredArgsConstructor @Slf4j
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String REQUEST_MESSAGE = "등록된 계정이면 비밀번호 재설정 링크를 이메일로 발송합니다. 메일을 확인해주세요.";
    private final JdbcTemplate jdbc;
    private final ApiService mail;
    @Value("${server.domain}") private String publicUrl;

    private record Account(long uid, String email, String password, String blocked) {}
    private record Challenge(long uid, String version, Timestamp expires, Timestamp created) {}
    private static String version(Account user) { return PasswordVersion.of(Objects.toString(user.password(), "")); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("유효하지 않거나 만료된 링크입니다. 비밀번호 찾기를 다시 진행해주세요."); }
    private List<Account> account(String where, Object value) {
        return jdbc.query("SELECT user_uid,user_email,user_pwd,bl_yn FROM seinfo_vendor_manager WHERE " + where + "=? FOR UPDATE",
            (rs,n) -> new Account(rs.getLong("user_uid"),rs.getString("user_email"),rs.getString("user_pwd"),rs.getString("bl_yn")), value);
    }
    private String resetUrl(String token) {
        URI base = URI.create(publicUrl);
        if (!("https".equalsIgnoreCase(base.getScheme()) || "http".equalsIgnoreCase(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalStateException("SEINFO_PUBLIC_URL에 올바른 서비스 주소를 설정해주세요.");
        }
        return publicUrl.replaceAll("/+$", "") + "/login/passwordReset#token=" + token;
    }
    @Transactional
    public void request(String requestedId) {
        String loginId = LoginIdPolicy.normalize(requestedId);
        if (!LoginIdPolicy.isValid(loginId)) return;
        List<Account> found = account("login_id", loginId);
        if (found.isEmpty() || "Y".equals(found.get(0).blocked())) return;
        Account user = found.get(0);
        Instant now = Instant.now();
        List<Timestamp> recent = jdbc.query("SELECT created_at FROM seinfo_password_reset WHERE user_uid=?",
            (rs,n) -> rs.getTimestamp(1), user.uid());
        if (!recent.isEmpty() && recent.get(0).toInstant().isAfter(now.minusSeconds(60))) return;
        byte[] random = new byte[32]; RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String url = resetUrl(token); // Only the configured URL is used; never the request Host header.
        jdbc.update("DELETE FROM seinfo_password_reset WHERE user_uid=?", user.uid());
        jdbc.update("INSERT INTO seinfo_password_reset(reset_key,user_uid,password_version,created_at,expires_at) VALUES (?,?,?,?,?)",
            PasswordVersion.of(token), user.uid(), version(user), Timestamp.from(now), Timestamp.from(now.plusSeconds(900)));
        Map<String,Object> message = new HashMap<>(Map.of("userEmail",user.email(),"mailTitle","[SE정보기술] 비밀번호 재설정 안내","resetUrl",url));
        Runnable dispatch = () -> {
            try { mail.sendMail(message, "seinfo/mail/mailPasswordReset"); }
            catch (Exception e) { log.warn("비밀번호 재설정 메일 전송을 시작하지 못했습니다."); }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { dispatch.run(); }
            });
        } else dispatch.run();
    }

    @Transactional
    public void complete(String token, String password) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        if (password == null || password.length() < 8 || password.length() > 20
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")
                || password.chars().noneMatch(c -> "!@#$%^&*(),.?\":{}|<>".indexOf(c) >= 0)) {
            throw new IllegalArgumentException("비밀번호는 영문·숫자·특수문자를 포함한 8~20자로 입력해주세요.");
        }
        String hash = PasswordVersion.of(token);
        List<Long> owners = jdbc.query("SELECT user_uid FROM seinfo_password_reset WHERE reset_key=?", (rs,n) -> rs.getLong(1), hash);
        if (owners.isEmpty()) throw invalid();
        // Lock in the same order as request(): account first, then its challenge.
        List<Account> found = account("user_uid", owners.get(0));
        if (found.isEmpty() || "Y".equals(found.get(0).blocked())) throw invalid();
        Account user = found.get(0);
        List<Challenge> rows = jdbc.query("SELECT user_uid,password_version,expires_at,created_at FROM seinfo_password_reset WHERE reset_key=? AND user_uid=? FOR UPDATE",
            (rs,n) -> new Challenge(rs.getLong("user_uid"),rs.getString("password_version"),rs.getTimestamp("expires_at"),rs.getTimestamp("created_at")), hash,user.uid());
        if (rows.isEmpty() || !rows.get(0).expires().toInstant().isAfter(Instant.now()) || !version(user).equals(rows.get(0).version())) throw invalid();
        if (user.password() != null && BCrypt.checkpw(password,user.password())) {
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요.");
        }
        String encoded = BCrypt.hashpw(password, BCrypt.gensalt());
        if (jdbc.update("UPDATE seinfo_vendor_manager SET user_pwd=?,user_pwd_init_yn='N',fail_cnt=0,fail_dt=NULL,user_pwd_ch_d=CURRENT_DATE,user_login_d=CURRENT_DATE,mod_dt=CURRENT_TIMESTAMP,mod_user_id=? WHERE user_uid=?",
                encoded,user.email(),user.uid()) != 1) throw invalid();
        jdbc.update("DELETE FROM zzz_token_access WHERE token_user_id=?",user.email());
        jdbc.update("DELETE FROM seinfo_password_reset WHERE user_uid=?",user.uid());
    }
}
