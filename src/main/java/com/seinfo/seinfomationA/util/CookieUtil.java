package com.seinfo.seinfomationA.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class CookieUtil {

    /**
     * 쿠키 생성
     * @param cookieName
     * @param cookieValue
     * @param res
     */

    public void createCookie(String cookieName, String cookieValue, HttpServletResponse res, String serverType) {

        if("local".equals(serverType)) {
            ResponseCookie resCookie = ResponseCookie.from(cookieName, cookieValue)
                    .path("/")
                    .sameSite("None")
                    .httpOnly(false)
                    .secure(true)
                    .maxAge(60 * 60 * 24 * 1) // 유효기간 1일
                    .build();
            res.addHeader("Set-Cookie", resCookie.toString());
        }else {
            ResponseCookie resCookie = ResponseCookie.from(cookieName, cookieValue)
                    .path("/")
                    .sameSite("None")
                    .httpOnly(false)
                    .secure(true)
                    .maxAge(60 * 60 * 24 * 1) // 유효기간 1일
                    .domain("seinfo.ddns.net") // 도메인 주소
                    .build();
            res.addHeader("Set-Cookie", resCookie.toString());
        }
    }

    /**
     * 쿠키 삭제
     * @param cookieName
     * @param res
     */

    public void removeCookie(String cookieName, HttpServletResponse res, String serverType) {
        // 단일 도메인일 경우 local로 해도 됨.
        if("local".equals(serverType)) {
            ResponseCookie deleteCookie = ResponseCookie.from(cookieName, "")
                    .path("/")
                    .sameSite("None")
                    .httpOnly(false)
                    .secure(true)
                    .maxAge(0)
                    .build();
            res.addHeader("Set-Cookie", deleteCookie.toString());
        }else {
            ResponseCookie deleteCookie = ResponseCookie.from(cookieName, "")
                    .path("/")
                    .sameSite("None")
                    .httpOnly(false)
                    .secure(true)
                    .maxAge(0)
                    .domain("seinfo.ddns.net") // 도메인 주소
                    .build();
            res.addHeader("Set-Cookie", deleteCookie.toString());
        }
    }
}
