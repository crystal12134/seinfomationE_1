package com.seinfo.seinfomationA;

import com.seinfo.seinfomationA.component.admin.mapper.ManageTeamMapper;
import com.seinfo.seinfomationA.component.admin.model.TeamMangerModel;
import com.seinfo.seinfomationA.component.admin.service.impl.ManageTeamServiceImpl;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TeamIdFeatureTest {
    ManageTeamMapper mapper; ManageTeamServiceImpl service;
    @BeforeEach void setup() {
        mapper = mock(ManageTeamMapper.class);
        service = new ManageTeamServiceImpl(new ResponseEntityUtil(new MockHttpServletRequest()), mapper);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.invalid", "", List.of()));
    }
    TeamMangerModel member(int uid, String loginId, String email) {
        TeamMangerModel m = new TeamMangerModel(); m.setUserUid(uid); m.setLoginId(loginId); m.setUserEmail(email); return m;
    }
    @Test void existingMemberUsesPkUpdateWithoutChangingPublicIdOrEmail() throws Exception {
        TeamMangerModel m = member(19, "member19", "registered@example.invalid");
        when(mapper.selectLoginIdentity(anyMap())).thenReturn(Map.of("loginId","member19","userEmail","registered@example.invalid"));
        service.upsertTeamManager(List.of(m));
        verify(mapper).updateTeamMember(m); verify(mapper, never()).insertTeamMember(any());
    }
    @Test void duplicateIdsWithinNewBatchFailBeforeAnyDatabaseWrite() {
        when(mapper.selectLoginIdOwner(anyMap())).thenReturn(null);
        when(mapper.selectTeamEmailOwner(anyMap())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.upsertTeamManager(List.of(
                member(0,"newuser1","one@example.invalid"), member(0,"NEWUSER1","two@example.invalid"))));
        verify(mapper, never()).insertTeamMember(any()); verify(mapper, never()).updateTeamMember(any());
    }
    @Test void modifyingExistingLoginIdIsRejected() {
        when(mapper.selectLoginIdentity(anyMap())).thenReturn(Map.of("loginId","member19","userEmail","registered@example.invalid"));
        assertThrows(IllegalArgumentException.class, () -> service.upsertTeamManager(List.of(
                member(19,"another1","registered@example.invalid"))));
        verify(mapper, never()).updateTeamMember(any());
    }
}
