package com.seinfo.seinfomationA.component.admin.service.impl;

import com.seinfo.seinfomationA.component.admin.mapper.ManageTeamMapper;
import com.seinfo.seinfomationA.component.admin.model.TeamMangerModel;
import com.seinfo.seinfomationA.component.admin.model.TeamModel;
import com.seinfo.seinfomationA.component.admin.service.ManageTeamService;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageTeamServiceImpl implements ManageTeamService {

    private final ResponseEntityUtil responseEntityUtil;
    private final ManageTeamMapper manageTeamMapper;


    @Override
    public ModelAndView loadTeamManagePage() {
        ModelAndView mv = new ModelAndView();
        List<Map<String, Object>> teamMemberList = manageTeamMapper.selectTeamMemberList(null);
        List<Map<String, Object>> teamList = manageTeamMapper.selectTeamList(null);
        mv.setViewName("seinfo/admin/teamManage");
        mv.addObject("teamList", teamList);
        mv.addObject("teamMemberList", teamMemberList);
        return mv;
    }


    @Override
    public ResponseEntity<?> reloadTeamManagerList(Map<String, Object> param) {

        List<Map<String, Object>> teamList = manageTeamMapper.selectTeamMemberList(param);

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", teamList);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteTeamManger(List<TeamMangerModel> list) throws Exception {
        for(TeamMangerModel data : list){
            manageTeamMapper.deleteTeamManger(data.getUserUid());
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "삭제되었습니다.");
    }

    @Override
    @Transactional
    public ResponseEntity<?> upsertTeamManager(List<TeamMangerModel> list) throws Exception {
        String regUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        java.util.Set<String> batchLoginIds = new java.util.HashSet<>();
        java.util.Set<String> batchEmails = new java.util.HashSet<>();
        for(TeamMangerModel data : list){
            if (data.getUserEmail() == null || data.getUserEmail().isBlank()) {
                throw new IllegalArgumentException("이메일을 입력해주세요.");
            }
            data.setUserEmail(data.getUserEmail().trim());
            if (!batchEmails.add(data.getUserEmail().toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException("저장 목록에 중복 이메일이 있습니다.");
            }
            if (data.getUserUid() > 0) {
                Map<String, Object> current = manageTeamMapper.selectLoginIdentity(Map.of("userUid", data.getUserUid()));
                if (current == null) throw new IllegalArgumentException("등록되지 않은 팀원입니다.");
                // 기존 회원의 공개 아이디와 내부 연결용 이메일은 변경하지 않습니다.
                if (data.getLoginId() != null && !data.getLoginId().isBlank()
                        && !data.getLoginId().equalsIgnoreCase(current.get("loginId").toString())) {
                    throw new IllegalArgumentException("기존 회원의 로그인 아이디는 변경할 수 없습니다.");
                }
                if (!data.getUserEmail().equalsIgnoreCase(current.get("userEmail").toString())) {
                    throw new IllegalArgumentException("기존 회원의 이메일은 팀원관리에서 변경할 수 없습니다.");
                }
                data.setLoginId(current.get("loginId").toString());
            } else {
                String loginId = com.seinfo.seinfomationA.util.LoginIdPolicy.requireValid(data.getLoginId());
                if (manageTeamMapper.selectLoginIdOwner(Map.of("loginId", loginId)) != null) {
                    throw new IllegalArgumentException("이미 사용 중인 로그인 아이디입니다.");
                }
                if (manageTeamMapper.selectTeamEmailOwner(Map.of("userEmail", data.getUserEmail())) != null) {
                    throw new IllegalArgumentException("이미 가입된 이메일입니다.");
                }
                data.setLoginId(loginId);
            }
            if (!batchLoginIds.add(data.getLoginId())) {
                throw new IllegalArgumentException("저장 목록에 중복 로그인 아이디가 있습니다.");
            }
            data.setRegUserId(regUserId);
        }
        for (TeamMangerModel data : list) {
            if (data.getUserUid() > 0) manageTeamMapper.updateTeamMember(data);
            else manageTeamMapper.insertTeamMember(data);
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "저장되었습니다.");
    }

    @Override
    public ModelAndView loadTeamListPage() {
        ModelAndView mv = new ModelAndView();
        List<Map<String, Object>> teamMemberList = manageTeamMapper.selectMemberList();
        List<Map<String, Object>> teamList = manageTeamMapper.selectTeamList(null);
        mv.setViewName("seinfo/admin/teamlist");
        mv.addObject("teamMemberList", teamMemberList);
        mv.addObject("teamList", teamList);
        return mv;
    }

    @Override
    public ResponseEntity<?> reloadTeamList(Map<String, Object> param) {
        List<Map<String, Object>> teamList = manageTeamMapper.selectTeamList(param);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", teamList);
    }


    @Override
    public ResponseEntity<?> upsertTeam(List<TeamModel> list) throws Exception {
        manageTeamMapper.upsertTeam(list);
        for(TeamModel data : list){
            if(data.getTeamManager() != null) {
                Map<String, Object> param = new HashMap<>();
                param.put("teamNo", data.getTeamNo());
                param.put("userUid", data.getTeamManager());
                manageTeamMapper.updateTeamManger(param);
            }
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "저장되었습니다.");
    }

    @Override
    public ResponseEntity<?> deleteTeam(List<TeamModel> list) throws Exception {
        for(TeamModel data : list){
            manageTeamMapper.deleteTeam(data.getTeamNo());
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "삭제되었습니다.");
    }
}
