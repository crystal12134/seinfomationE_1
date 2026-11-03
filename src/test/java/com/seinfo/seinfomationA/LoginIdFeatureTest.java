package com.seinfo.seinfomationA;

import com.seinfo.seinfomationA.component.main.mapper.LoginMapper;
import com.seinfo.seinfomationA.component.main.model.LoginModel;
import com.seinfo.seinfomationA.component.main.service.impl.LoginServiceImpl;
import com.seinfo.seinfomationA.component.main.service.impl.PasswordResetService;
import com.seinfo.seinfomationA.util.LoginIdPolicy;
import com.seinfo.seinfomationA.util.api.ApiService;
import com.seinfo.seinfomationA.util.response.CoreWebApiResponseModel;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.token.TokenMapper;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginIdFeatureTest {
    LoginMapper mapper;
    AuthenticationManager authentication;
    TokenUtils tokens;
    ApiService mail;
    MockHttpServletRequest request;
    LoginServiceImpl service;
    PasswordResetService resets;

    @BeforeEach void setup() {
        mapper = mock(LoginMapper.class);
        authentication = mock(AuthenticationManager.class);
        tokens = mock(TokenUtils.class);
        mail = mock(ApiService.class);
        resets = mock(PasswordResetService.class);
        request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        service = new LoginServiceImpl(new MockHttpServletResponse(), request, authentication,
                mapper, mock(TokenMapper.class), new ResponseEntityUtil(request), tokens, mail, resets);
        ReflectionTestUtils.setField(service, "serverType", "local");
        ReflectionTestUtils.setField(service, "checkDupIP", "false");
        SecurityContextHolder.clearContext();
    }

    Map<String, Object> user(boolean temporary) {
        Map<String, Object> user = new HashMap<>();
        user.put("userPwd", "test-encoded-password");
        user.put("userUid", 19); user.put("loginId", "member19");
        user.put("userEmail", "registered@example.invalid");
        user.put("userNm", "테스트"); user.put("userRole", "USER");
        user.put("companyNo", "test-company"); user.put("companyNm", "테스트회사");
        user.put("blYn", "N"); user.put("failCnt", 0);
        user.put("unLocking", "Y"); user.put("waitingTime", 0);
        user.put("userPwdInitYn", temporary ? "Y" : "N");
        user.put("userPwdChD", LocalDate.now().toString());
        user.put("userLoginD", LocalDate.now().toString());
        return user;
    }

    @Test void normalizesAndRejectsEmailAsLoginId() {
        assertEquals("seinfo01", LoginIdPolicy.requireValid(" SEINFO01 "));
        assertFalse(LoginIdPolicy.isValid("test@example.com"));
        for (String invalid : new String[]{"", "abc", "1abc", "한글아이디", "ab-cd", "a".repeat(21)}) {
            assertThrows(IllegalArgumentException.class, () -> LoginIdPolicy.requireValid(invalid));
        }
    }

    @Test void invalidIdDoesNotReachPasswordAuthentication() throws Exception {
        LoginModel model = new LoginModel(); model.setId("test@example.com"); model.setPassword("Password9!");
        service.loginProcess(model);
        verifyNoInteractions(mapper, authentication);
    }

    @Test void authenticatesResolvedEmailButLooksUpPublicId() throws Exception {
        when(mapper.selectUsrInfo(anyMap())).thenReturn(user(false));
        when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("test"));
        LoginModel model = new LoginModel(); model.setId(" MEMBER19 "); model.setPassword("Wrong9!pass");
        service.loginProcess(model);
        verify(mapper).selectUsrInfo(argThat(p -> "member19".equals(p.get("loginId"))));
        verify(authentication).authenticate(argThat(a -> "registered@example.invalid".equals(a.getName())));
        verify(mapper).updateUsrLoginFail(argThat(p -> "registered@example.invalid".equals(p.get("userEmail"))));
    }

    @Test void successfulLoginKeepsInternalIdentityAndAddsPublicClaim() throws Exception {
        when(mapper.selectUsrInfo(anyMap())).thenReturn(user(false));
        when(authentication.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("registered@example.invalid", "", java.util.List.of()));
        when(tokens.generateToken(anyMap())).thenReturn("test-token");
        LoginModel model = new LoginModel(); model.setId("member19"); model.setPassword("Password9!");
        CoreWebApiResponseModel result = (CoreWebApiResponseModel) service.loginProcess(model).getBody();
        assertEquals("00", result.getProcess_code());
        verify(tokens).generateToken(argThat(c -> "member19".equals(c.get("LOGIN_ID"))
                && "registered@example.invalid".equals(c.get(TokenUtils.TOKEN_USER_ID))));
        verify(mapper).insertAccessLog(argThat(p -> "registered@example.invalid".equals(p.get("userEmail"))));
    }

    @Test void recoveryDelegatesWithoutChangingPasswordOrUsingCallerEmail() throws Exception {
        Map<String,Object> params = new HashMap<>(Map.of("id","MEMBER19","userEmail","attacker@example.invalid"));
        CoreWebApiResponseModel result = (CoreWebApiResponseModel)service.pwdFindProcess(params).getBody();
        verify(resets).request("MEMBER19");
        verify(mapper,never()).updateUsrTempPwd(anyMap());
        verifyNoInteractions(mail);
        assertEquals("00",result.getProcess_code());
        assertFalse(((Map<?,?>)result.getData()).containsKey("tempPassword"));
    }

    @Test void unknownAccountDoesNotChangePasswordOrSendMail() throws Exception {
        when(mapper.selectUsrInfo(anyMap())).thenReturn(null);
        service.pwdFindProcess(new HashMap<>(Map.of("id", "missing1")));
        verify(mapper, never()).updateUsrTempPwd(anyMap()); verifyNoInteractions(mail);
    }

    @Test void unlockAlsoUsesSecureRecoveryWithoutImmediatePasswordChange() throws Exception {
        service.unlockPwdProcess(new HashMap<>(Map.of("id","member19")));
        verify(resets).request("member19");
        verify(mapper,never()).updateUsrTempPwd(anyMap());
        verifyNoInteractions(mail);
    }

    @Test void arbitraryUidCannotChangePasswordWithoutTemporaryLogin() throws Exception {
        LoginModel model = new LoginModel(); model.setUserUid("19"); model.setNewPassword("Changed9!pass");
        service.changePwdProcess(model);
        verify(mapper, never()).findUsrInfoByUid(anyMap());
        verify(mapper, never()).updateUsrPwd(anyMap());
    }

    @Test void temporaryLoginAuthorizesOnePasswordChangeForThatSession() throws Exception {
        when(mapper.selectUsrInfo(anyMap())).thenReturn(user(true));
        when(authentication.authenticate(any())).thenReturn(mock(Authentication.class));
        LoginModel login = new LoginModel(); login.setId("member19"); login.setPassword("Temp9!pass");
        service.loginProcess(login);
        assertEquals("19", request.getSession().getAttribute("TEMP_PASSWORD_USER_UID"));
        when(mapper.findUsrInfoByUid(anyMap())).thenReturn(Map.of("userEmail", "registered@example.invalid",
                "userPwd", BCrypt.hashpw("Temp9!pass", BCrypt.gensalt())));
        LoginModel change = new LoginModel(); change.setUserUid("19"); change.setNewPassword("Changed9!pass");
        service.changePwdProcess(change);
        verify(mapper).updateUsrPwd(argThat(p -> "registered@example.invalid".equals(p.get("userEmail"))));
        assertNull(request.getSession().getAttribute("TEMP_PASSWORD_USER_UID"));
        service.changePwdProcess(change);
        verify(mapper, times(1)).updateUsrPwd(anyMap());
    }
}
