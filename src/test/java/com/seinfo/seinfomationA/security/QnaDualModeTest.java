package com.seinfo.seinfomationA.security;

import com.seinfo.seinfomationA.component.board.Model.QnaModel;
import com.seinfo.seinfomationA.component.board.mapper.QnaMapper;
import com.seinfo.seinfomationA.component.board.service.impl.QnaServiceImpl;
import com.seinfo.seinfomationA.util.response.*;
import com.seinfo.seinfomationA.util.security.AuthUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QnaDualModeTest {
    QnaMapper mapper; TokenUtils tokens; QnaServiceImpl service;
    final String actor = "castle777@example.invalid";
    @BeforeEach void setup() {
        mapper = mock(QnaMapper.class); tokens = mock(TokenUtils.class);
        // Deliberately stale token identity: writes must use the authenticated principal.
        when(tokens.getClaimsByValue("USER_ID")).thenReturn("other@example.invalid");
        service = new QnaServiceImpl(tokens, new ResponseEntityUtil(new MockHttpServletRequest()), mapper, new AuthUtil(tokens));
        role("ADMIN");
        when(mapper.selectQnaList(anyMap())).thenReturn(List.of());
    }
    void role(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,"",AuthorityUtils.createAuthorityList("ROLE_"+role)));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    QnaModel question(String mode, String id) {
        var model = new QnaModel(); model.setModifyMode(mode); model.setNoticeNo(id);
        model.setNoticeTitle("test"); model.setNoticeContents("test content");
        model.setUserType("ADMIN"); model.setAdminYn("Y"); return model;
    }
    @Test void adminMyPageIsMemberModeAndManagementRequiresExplicitMode() {
        var page=service.loadQnaPage(1,null);
        assertEquals("N",page.getModel().get("adminYn"));
        assertEquals("member",page.getModel().get("qnaMode"));
        verify(mapper).selectQnaList(argThat(p -> Boolean.FALSE.equals(p.get("isAdmin")) && actor.equals(p.get("userEmail"))));
        clearInvocations(mapper);
        var manage=service.loadQnaPage(1,null,"admin");
        assertEquals("Y",manage.getModel().get("adminYn"));
        verify(mapper).selectQnaList(argThat(p -> Boolean.TRUE.equals(p.get("isAdmin"))));
        clearInvocations(mapper);
        service.reloadQnaPage(Map.of("page",1,"isAdmin",true,"adminYn","Y","userEmail","other@example.invalid"));
        verify(mapper).selectQnaList(argThat(p -> Boolean.FALSE.equals(p.get("isAdmin")) && actor.equals(p.get("userEmail"))));
    }
    @Test void adminAndUserCanCreateOwnQuestionWithoutTrustingBrowserRole() {
        for (String role : List.of("ADMIN","USER")) {
            role(role); service.registQuestion(question("R",null));
        }
        verify(mapper,times(2)).insertQna(argThat(p -> actor.equals(p.get("userEmail")) && Boolean.FALSE.equals(p.get("isAdmin"))));
    }
    @Test void adminCanEditOwnQuestionButCannotEditOrDeleteAnotherAsMember() {
        when(mapper.selectQnaDetail(anyMap())).thenReturn(Map.of("userEmail",actor));
        service.registQuestion(question("RU","own"));
        verify(mapper).updateQna(argThat(p -> actor.equals(p.get("userEmail")) && Boolean.FALSE.equals(p.get("isAdmin"))));
        when(mapper.selectQnaDetail(anyMap())).thenReturn(Map.of("userEmail","other@example.invalid"));
        assertThrows(AccessDeniedException.class,()->service.registQuestion(question("RU","foreign")));
        assertThrows(AccessDeniedException.class,()->service.registQuestion(question("RD","foreign")));
        verify(mapper,never()).updateQnaDel(anyMap());
    }
    @Test void adminMemberDetailIsOwnOnlyAndAdminModeCanReadAnother() {
        when(mapper.selectQnaDetail(anyMap())).thenAnswer(call -> new HashMap<>(Map.of("userEmail","other@example.invalid")));
        var denied=(CoreWebApiResponseModel)service.getQuestionDetail(Map.of("noticeNo","foreign")).getBody();
        assertNotEquals("00",denied.getProcess_code());
        var allowed=(CoreWebApiResponseModel)service.getQuestionDetail(Map.of("noticeNo","foreign","qnaMode","admin")).getBody();
        assertEquals("00",allowed.getProcess_code());
    }
    @Test void ordinaryUserEmployeeAndAnonymousCannotEnterAdminModeOrAnswer() {
        var model=new QnaModel(); model.setModifyMode("A"); model.setNoticeNo("foreign");
        model.setAnswerTitle("reply"); model.setAnswerContents("reply");
        for (String role : List.of("USER","EMPLOYEE")) {
            role(role);
            assertThrows(AccessDeniedException.class,()->service.loadQnaPage(1,null,"admin"));
            assertThrows(AccessDeniedException.class,()->service.reloadQnaPage(Map.of("page",1,"qnaMode","admin")));
            assertThrows(AccessDeniedException.class,()->service.getQuestionDetail(Map.of("noticeNo","foreign","qnaMode","admin")));
            assertThrows(AccessDeniedException.class,()->service.registReply(model));
        }
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class,()->service.registQuestion(question("R",null)));
        verify(mapper,never()).updateAnswer(anyMap());
    }
    @Test void realAdminCanAnswerUsingAuthenticatedIdentity() {
        var model=new QnaModel(); model.setModifyMode("A"); model.setNoticeNo("foreign");
        model.setAnswerTitle("reply"); model.setAnswerContents("reply");
        service.registReply(model);
        verify(mapper).updateAnswer(argThat(p -> Boolean.TRUE.equals(p.get("isAdmin")) && actor.equals(p.get("userId"))));
    }
}
