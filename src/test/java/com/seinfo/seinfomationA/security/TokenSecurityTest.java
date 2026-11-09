package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.token.*;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TokenSecurityTest {
 TokenMapper mapper;TokenUtils tokens;
 @BeforeEach void setup(){mapper=mock(TokenMapper.class);tokens=new TokenUtils(new MockHttpServletRequest(),mapper);ReflectionTestUtils.setField(tokens,"secret",Base64.getEncoder().encodeToString(new byte[32]));ReflectionTestUtils.setField(tokens,"expiration",1800L);}
 String active(){String t=tokens.generateToken(new HashMap<>(Map.of("USER_ID","alice")));when(mapper.selectTokenAccess(anyMap())).thenReturn(Map.of("tokenKey",com.seinfo.seinfomationA.util.security.TokenFingerprint.of(t)));return t;}
 @Test void acceptsSignedCurrentToken(){String t=active();assertEquals("alice",tokens.getClaimsByValue(t,"USER_ID"));}
 @Test void rejectsExpiredInsteadOfReturningItsIdentity(){ReflectionTestUtils.setField(tokens,"expiration",-1L);String t=active();assertThrows(JwtException.class,()->tokens.getClaimsByValue(t,"USER_ID"));}
 @Test void rejectsRevokedToken(){String t=active();when(mapper.countDeniedToken(com.seinfo.seinfomationA.util.security.TokenFingerprint.of(t))).thenReturn(1);assertThrows(JwtException.class,()->tokens.getClaimsByValue(t,"USER_ID"));}
 @Test void rejectsReplacedToken(){String t=active();when(mapper.selectTokenAccess(anyMap())).thenReturn(Map.of("tokenKey","replacement"));assertThrows(JwtException.class,()->tokens.getClaimsByValue(t,"USER_ID"));}
 @Test void rejectsModifiedSignature(){String t=active();String[] p=t.split("\\.");String s=p[0]+"."+p[1]+"."+(p[2].startsWith("A")?"B":"A")+p[2].substring(1);assertThrows(JwtException.class,()->tokens.getClaimsByValue(s,"USER_ID"));}
 @Test void twoGeneratedTokensAreDifferent(){String a=active();String b=active();assertNotEquals(a,b);}
}
