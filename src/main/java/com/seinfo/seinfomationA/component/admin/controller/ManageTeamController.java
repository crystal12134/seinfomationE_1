package com.seinfo.seinfomationA.component.admin.controller;

import com.seinfo.seinfomationA.component.admin.model.TeamMangerModel;
import com.seinfo.seinfomationA.component.admin.model.TeamModel;
import com.seinfo.seinfomationA.component.admin.service.ManageTeamService;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class ManageTeamController {

    private final ManageTeamService  manageTeamService;
    private final ResponseEntityUtil responseEntityUtil;

    @GetMapping("/teamManage")
    public ModelAndView teamManagePage() {
        return manageTeamService.loadTeamManagePage();
    }

    @PostMapping("/reloadTeamManage")
    public ResponseEntity<?> reloadTeamManage(@RequestBody Map<String, Object> param) {
        return manageTeamService.reloadTeamManagerList(param);
    }

    @PostMapping("/saveTeamMangerData")
    public ResponseEntity<?> saveTeamMangerData(@RequestBody List<TeamMangerModel> list) throws Exception {
        try {
            return manageTeamService.upsertTeamManager(list);
        }catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("saveTeamMangerData >>> Exception >>> {}", e.getMessage());
            Map<String, Object> rtnMap = new HashMap<>();
            rtnMap.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "S001", rtnMap);
        }
    }

    @PostMapping("/deleteTeamManagerData")
    public ResponseEntity<?> deleteTeamManagerData(@RequestBody List<TeamMangerModel> list) throws Exception {
        try {
            return manageTeamService.deleteTeamManger(list);
        }catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("deleteTeamManagerData >>> Exception >>> {}", e.getMessage());
            Map<String, Object> rtnMap = new HashMap<String, Object>();
            rtnMap.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "S001", rtnMap);
        }
    }

    @GetMapping("/teamList")
    public ModelAndView teamListPage() {
        return manageTeamService.loadTeamListPage();
    }

    @PostMapping("/reloadTeamList")
    public ResponseEntity<?> reloadTeamList(@RequestBody Map<String, Object> param) {
        return manageTeamService.reloadTeamList(param);
    }

    @PostMapping("/saveTeamData")
    public ResponseEntity<?> saveTeamData(@RequestBody List<TeamModel> list) throws Exception {
        try {
            return manageTeamService.upsertTeam(list);
        }catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("saveTeamData >>> Exception >>> {}", e.getMessage());
            Map<String, Object> rtnMap = new HashMap<>();
            rtnMap.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "S001", rtnMap);
        }
    }

    @PostMapping("/deleteTeamData")
    public ResponseEntity<?> deleteTeamData(@RequestBody List<TeamModel> list) throws Exception {
        try {
            return manageTeamService.deleteTeam(list);
        }catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("deleteTeamData >>> Exception >>> {}", e.getMessage());
            Map<String, Object> rtnMap = new HashMap<String, Object>();
            rtnMap.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "S001", rtnMap);
        }
    }
}
