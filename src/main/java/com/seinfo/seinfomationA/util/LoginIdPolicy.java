package com.seinfo.seinfomationA.util;

import java.util.Locale;

/** 공개 로그인 아이디만 정규화합니다. 내부 회원 연결용 이메일은 변경하지 않습니다. */
public final class LoginIdPolicy {
    private LoginIdPolicy() { }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        return value != null && value.matches("^[a-z][a-z0-9_]{3,19}$");
    }

    public static String requireValid(String value) {
        String loginId = normalize(value);
        if (!isValid(loginId)) {
            throw new IllegalArgumentException("아이디는 영문자로 시작하는 영문 소문자·숫자·밑줄(_) 4~20자로 입력해주세요.");
        }
        return loginId;
    }
}
