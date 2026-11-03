package com.seinfo.seinfomationA.security;
import com.seinfo.seinfomationA.util.token.*;
import com.seinfo.seinfomationA.util.security.PasswordVersion;
import com.seinfo.seinfomationA.component.main.mapper.LoginMapper;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AuthenticationFilterTest {
 TokenUtils tokens;LoginMapper mapper;JwtAuthenticationFilter filter;MockHttpServletRequest request;
 Map<String,Object> user;
 @BeforeEach void setup(){
  SecurityContextHolder.clearContext();tokens=mock(TokenUtils.class);mapper=mock(LoginMapper.class);filter=new JwtAuthenticationFilter(tokens,mapper);
  request=new MockHttpServletRequest("GET","/request/requestList");
  when(tokens.getTokenFromCookie(any())).thenReturn("token");when(tokens.getTokenFromCookie()).thenReturn("token");
  when(tokens.getClaimsByValue("EXPIRATION_VALID")).thenReturn(false);when(tokens.getClaimsByValue("USER_ID")).thenReturn("alice");
  when(tokens.getClaimsByValue("PWD_VERSION")).thenReturn(PasswordVersion.of("encoded-password"));
  user=new HashMap<>(Map.of("userUid",1,"userEmail","alice","userPwd","encoded-password","userRole","USER","blYn","N","userPwdInitYn","N"));
  when(mapper.selectUserByEmail("alice")).thenReturn(user);
 }
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void authenticatesActiveAccountWithoutCreatingSession() throws Exception {filter.doFilter(request,new MockHttpServletResponse(),mock(FilterChain.class));assertEquals("alice",SecurityContextHolder.getContext().getAuthentication().getName());assertNull(request.getSession(false));}
 @Test void blockedAccountIsNotAuthenticated() throws Exception {user.put("blYn","Y");filter.doFilter(request,new MockHttpServletResponse(),mock(FilterChain.class));assertNull(SecurityContextHolder.getContext().getAuthentication());}
 @Test void passwordChangeInvalidatesOldToken() throws Exception {user.put("userPwd","changed-encoded-password");filter.doFilter(request,new MockHttpServletResponse(),mock(FilterChain.class));assertNull(SecurityContextHolder.getContext().getAuthentication());}
 @Test void temporaryPasswordAccountCannotReuseFullSession() throws Exception {user.put("userPwdInitYn","Y");filter.doFilter(request,new MockHttpServletResponse(),mock(FilterChain.class));assertNull(SecurityContextHolder.getContext().getAuthentication());}
}
