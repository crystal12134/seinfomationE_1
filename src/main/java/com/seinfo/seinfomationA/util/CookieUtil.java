package com.seinfo.seinfomationA.util;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
public class CookieUtil {
    public void createCookie(String name,String value,HttpServletResponse response,String serverType){
        response.addHeader("Set-Cookie",ResponseCookie.from(name,value).path("/").sameSite("Lax").httpOnly(true).secure(!"local".equals(serverType)).maxAge(1800).build().toString());
    }
    public void removeCookie(String name,HttpServletResponse response,String serverType){
        response.addHeader("Set-Cookie",ResponseCookie.from(name,"").path("/").sameSite("Lax").httpOnly(true).secure(!"local".equals(serverType)).maxAge(0).build().toString());
        // Expire the cookie domain used by the previous version as well.
        response.addHeader("Set-Cookie",ResponseCookie.from(name,"").path("/").domain("seinfo.ddns.net").sameSite("Lax").httpOnly(true).secure(true).maxAge(0).build().toString());
    }
}
