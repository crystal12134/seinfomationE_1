package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.CookieUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
class CookieSecurityTest {
 @Test void productionAuthCookieIsHttpOnlySecureAndLax(){var response=new MockHttpServletResponse();new CookieUtil().createCookie("X-AUTH-TOKEN","token",response,"production");String cookie=response.getHeader("Set-Cookie");assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Lax"));assertFalse(cookie.contains("Domain="));}
 @Test void localHttpStillWorksWithHttpOnly(){var response=new MockHttpServletResponse();new CookieUtil().createCookie("X-AUTH-TOKEN","token",response,"local");String cookie=response.getHeader("Set-Cookie");assertTrue(cookie.contains("HttpOnly"));assertFalse(cookie.contains("Secure"));}
}
