package com.seinfo.protoA.util;

import com.seinfo.protoA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComUtil {

    private final TokenUtils tokenUtils;

    /**
     * 토큰 유효성 체크
     */
    public Map<String, Object> getVaildateToken(String type) {
        Map<String, Object> resultMap = new HashMap<>();

        if(tokenUtils.getTokenFromCookie() == null) {
            resultMap.put("rtnToken", "N");
            resultMap.put("message", "잘못된 접근입니다. 로그인이 필요합니다.");
            return resultMap;
        }

        String userType = ("RTP".equals(type)) ? "01" : type;
        String tokenUserType = tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_TYPE) == null? "unknownuser"
                                : tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_TYPE).toString();
        if(!userType.equals(tokenUserType)) {
            resultMap.put("rtnToken", "N");
            resultMap.put("message", "인증되지 않은 사용자입니다. 로그인이 필요합니다.");
            return resultMap;
        }

        if("".equals(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID))) {
            resultMap.put("rtnToken", "N");
            resultMap.put("message", "사용자ID가 유요하지 않습니다. 로그인이 필요합니다.");
            return resultMap;
        }

        String tokenUserId = tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID) == null?  ""
                : tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID).toString();
        resultMap.put("rtnToken", "Y");
        resultMap.put("tokenUserId", tokenUserId);
        return resultMap;
    }

    /**
     * html 태그 치환 : unEscape
     */

    public static String exchangeHtml(String htmlStr) {
        if(htmlStr == null) return htmlStr;

        htmlStr = htmlStr.replaceAll("&lt;", "<");
        htmlStr = htmlStr.replaceAll("&gt;", ">");
        htmlStr = htmlStr.replaceAll("&le", "<=");
        htmlStr = htmlStr.replaceAll("&ge;", ">=");
        htmlStr = htmlStr.replaceAll("&nbsp;", " ");
        htmlStr = htmlStr.replaceAll("&ne", "!=");
        htmlStr = htmlStr.replaceAll("&equals", "=");
        htmlStr = htmlStr.replaceAll("&amp;", "&");
        htmlStr = htmlStr.replaceAll("&quot;", "\"");
        htmlStr = htmlStr.replaceAll("&apos;", "\'");

        return htmlStr;
    }
}
