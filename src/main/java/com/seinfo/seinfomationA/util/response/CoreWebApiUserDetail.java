package com.seinfo.seinfomationA.util.response;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.*;

@Getter
public class CoreWebApiUserDetail implements UserDetails, Serializable {

    private static final Logger log = LoggerFactory.getLogger(CoreWebApiUserDetail.class);

    private int userUid;
    private String userEmail;
    private String userNm;
    private String userPwd;
    private String deptId;
    private String deptNm;
    private String companyNo;
    private String userType;
    private String companyOriginCd;
    private Collection<GrantedAuthority> authorities;

    public CoreWebApiUserDetail(Map<String, Object> userInfo) {
        this.userUid = Integer.parseInt(String.valueOf(userInfo.get("userUid")));
        this.userEmail = (userInfo.get("userEmail") != null)? String.valueOf(userInfo.get("userEmail")) : null;
        this.userNm = (userInfo.get("userNm") != null)? String.valueOf(userInfo.get("userNm")) : null;
        this.userPwd = (userInfo.get("userPwd") != null)? String.valueOf(userInfo.get("userPwd")) : null;
        this.deptId = (userInfo.get("deptId") != null)? String.valueOf(userInfo.get("deptId")) : null;
        this.deptNm = (userInfo.get("deptId") != null)? String.valueOf(userInfo.get("deptId")) : null;
        this.companyNo = (userInfo.get("companyNo") != null)? String.valueOf(userInfo.get("companyNo")) : null;
        this.userType =  (userInfo.get("userType") != null)? String.valueOf(userInfo.get("userType")) : null;
        this.companyOriginCd =  (userInfo.get("companyOriginCd") != null)? String.valueOf(userInfo.get("companyOriginCd")) : null;
        this.authorities = (userInfo.get("userRole") != null)? makeGrantedAuthority(Arrays.asList(String.valueOf(userInfo.get("userRole")).split(","))) : null;
    }

    private static List<GrantedAuthority> makeGrantedAuthority(List<String> roles){
        List<GrantedAuthority> list = new ArrayList<>();
        roles.forEach(role -> list.add(new SimpleGrantedAuthority("ROLE_"+role)));
        return list;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.userPwd;
    }

    @Override
    public String getUsername() {
        return this.userEmail;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
