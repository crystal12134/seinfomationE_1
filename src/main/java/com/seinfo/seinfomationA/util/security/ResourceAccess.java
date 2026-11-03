package com.seinfo.seinfomationA.util.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ResourceAccess {
    private final JdbcTemplate jdbc;
    public String email() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken)
            throw new AccessDeniedException("로그인이 필요합니다.");
        return a.getName();
    }
    public boolean admin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
    }
    public void requireAdmin() { email(); if (!admin()) deny(); }
    public void requireStaff() {
        email(); Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (!admin() && a.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals("ROLE_EMPLOYEE"))) deny();
    }
    public static String id(Object value) {
        String s = Objects.toString(value, "");
        if (!s.matches("[A-Za-z0-9_-]{1,120}")) throw new AccessDeniedException("잘못된 자료 번호입니다.");
        return s;
    }
    private boolean exists(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args) > 0; }
    public void request(Object number) {
        String user = email(); String n = id(number);
        if (!exists("SELECT COUNT(*) FROM seinfo_request r JOIN seinfo_vendor_manager v ON v.user_uid=r.user_uid WHERE r.request_no=? AND (?=1 OR v.user_email=?)", n, admin()?1:0, user)) deny();
    }
    public void post(Object number) {
        String user=email();
        if (!exists("SELECT COUNT(*) FROM seinfo_freeboard_posts WHERE freeboard_no=? AND (?=1 OR user_id=?)", id(number), admin()?1:0, user)) deny();
    }
    public void comment(Object number) {
        String user=email();
        if (!exists("SELECT COUNT(*) FROM seinfo_freeboard_comments WHERE comment_no=? AND (?=1 OR user_id=?)",id(number),admin()?1:0,user)) deny();
    }
    public void work(Object number) {
        requireStaff(); String user=email();
        if (!exists("SELECT COUNT(*) FROM seinfo_work WHERE id=? AND (?=1 OR user_email=?)",id(number),admin()?1:0,user)) deny();
    }
    public void fileGroup(Object group, boolean write, boolean allowNew) {
        String g=id(group);
        if (!write && exists("SELECT COUNT(*) FROM seinfo_notice WHERE mfile_group_id=? AND notice_type='N'",g)) return;
        String user=email();
        if (!exists("SELECT COUNT(*) FROM seinfo_mfile_group WHERE mfile_group_id=?",g)) {
            if(allowNew) return;
            deny();
        }
        if (admin() || exists("SELECT COUNT(*) FROM seinfo_mfile_group WHERE mfile_group_id=? AND reg_user_id=?",g,user)) return;
        if (!write && (exists("SELECT COUNT(*) FROM seinfo_request r JOIN seinfo_vendor_manager v ON v.user_uid=r.user_uid WHERE r.mfile_group_id=? AND v.user_email=?",g,user)
            || exists("SELECT COUNT(*) FROM seinfo_request_cont c JOIN seinfo_request r ON r.request_no=c.request_no JOIN seinfo_vendor_manager v ON v.user_uid=r.user_uid WHERE c.mfile_group_id=? AND v.user_email=?",g,user)
            || exists("SELECT COUNT(*) FROM seinfo_work WHERE mfile_group_id=? AND user_email=?",g,user))) return;
        deny();
    }
    public void attachedGroup(Object group) {
        String value=Objects.toString(group,"");
        if(!value.isBlank() && !value.equals("null")) fileGroup(value,true,false);
    }
    private static void deny() { throw new AccessDeniedException("해당 자료에 접근할 권한이 없습니다."); }
}
