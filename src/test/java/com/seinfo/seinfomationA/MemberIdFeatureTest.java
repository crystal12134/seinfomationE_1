package com.seinfo.seinfomationA;

import com.seinfo.seinfomationA.component.member.mapper.MemberMapper;
import com.seinfo.seinfomationA.component.member.mapper.DropInfoMapper;
import com.seinfo.seinfomationA.component.member.model.CompanyModel;
import com.seinfo.seinfomationA.component.member.model.MemberModel;
import com.seinfo.seinfomationA.component.member.service.impl.MemberServiceImpl;
import com.seinfo.seinfomationA.util.api.ApiService;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.response.CoreWebApiResponseModel;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import com.seinfo.seinfomationA.component.file.service.impl.TusServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MemberIdFeatureTest {
    @org.junit.jupiter.api.BeforeEach void bindSession() {
        com.seinfo.seinfomationA.util.EncriptUtil.configure("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest()));
    }
    @org.junit.jupiter.api.AfterEach void unbindSession() { org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }

    @Test void duplicatePublicIdIsRejectedBeforeInsert() {
        MemberMapper mapper = mock(MemberMapper.class);
        when(mapper.chkLoginIdDuplicate(anyMap())).thenReturn(1);
        MemberServiceImpl service = service(mapper, mock(ApiService.class));
        MemberModel member = member();
        assertThrows(IllegalArgumentException.class, () -> service.setMemberInfoAdd(member, company()));
        verify(mapper, never()).insertVendorManager(anyMap());
    }

    @Test void signupRequiresServerVerifiedEmailAndStoresNormalizedId() {
        MemberMapper mapper = mock(MemberMapper.class); ApiService mail = mock(ApiService.class);
        when(mapper.insertVendor(anyMap())).thenReturn(true);
        when(mapper.insertVendorManager(anyMap())).thenReturn(true);
        MemberServiceImpl service = service(mapper, mail);
        assertThrows(IllegalArgumentException.class, () -> service.setMemberInfoAdd(member(), company()));
        CoreWebApiResponseModel sentResult = (CoreWebApiResponseModel) service.emailCheckProcess("new@example.invalid").getBody();
        assertFalse(((Map<?, ?>) sentResult.getData()).containsKey("authNumber"));
        ArgumentCaptor<Map<String, Object>> capture = ArgumentCaptor.forClass(Map.class);
        verify(mail).sendMail(capture.capture(), eq("seinfo/mail/mailTemplate04"));
        String code = capture.getValue().get("mailContent").toString();
        service.checkAuthNumberProcess("new@example.invalid", code);
        service.setMemberInfoAdd(member(), company());
        verify(mapper).insertVendorManager(argThat(p -> "newuser01".equals(p.get("loginId"))
                && "new@example.invalid".equals(p.get("userEmail"))));
        assertThrows(IllegalArgumentException.class, () -> service.setMemberInfoAdd(member(), company()));
    }

    MemberServiceImpl service(MemberMapper mapper, ApiService mail) {
        return new MemberServiceImpl(mapper, mock(TokenUtils.class), mock(DropInfoMapper.class),
                new ResponseEntityUtil(new MockHttpServletRequest()), mock(TusServiceImpl.class), mail);
    }
    MemberModel member() {
        MemberModel m = new MemberModel(); m.setLoginId(" NEWUSER01 "); m.setUserEmail("new@example.invalid");
        m.setUserPwd("Password9!"); m.setUserNm("테스트"); m.setUserPhone("01000000000"); return m;
    }
    CompanyModel company() { CompanyModel c = new CompanyModel(); c.setCompanyNo("1234567890"); return c; }
}
