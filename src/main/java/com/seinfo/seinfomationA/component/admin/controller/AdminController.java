package com.seinfo.seinfomationA.component.admin.controller;

import com.seinfo.seinfomationA.component.admin.service.AdminService;
import com.seinfo.seinfomationA.util.ComUtil;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final ResponseEntityUtil responseEntityUtil;
    private final ComUtil comUtil;

    @GetMapping("/requestList")
    public ModelAndView requestListPageForAdmin(@RequestParam(defaultValue = "1") int page) {
        return adminService.getRequestList(page);
    }

    @GetMapping("/reloadRequestList")
    public ResponseEntity<?> reloadRequestListPageForAdmin(@RequestParam(defaultValue = "1") int page) {
        return adminService.reloadRequestList(page);
    }

    @GetMapping(value = "/requestDetail")
    public ModelAndView requestHistoryDetail(@RequestParam(required = false) String paramData) {
        Map<String, Object> param = comUtil.fnDecParam(paramData);
        return adminService.getRequestDetail(param);
    }

    /**
     * requestDetail 재조회
     */
    @PostMapping(value = "/reloadRequestDetail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reloadRequestDetail(@RequestBody Map<String, Object> param) {
        return adminService.reloadRequestDetail(param);
    }


    /**
     * 추가문의 불러오기
     */
    @PostMapping(value = "/getRequestCont", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRequestCont(@RequestBody Map<String, Object> param) {
        return adminService.getRequestCont(param);
    }

    @PostMapping(value = "/updateRequestState", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateRequestState(@RequestBody Map<String, Object> param) {
        try {
            return adminService.updateRequestState(param);
        }catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("updateProposalState >>> Exception >>> {}", e.getMessage());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "S001", e.getMessage());
        }
    }

    /**
     * 추가문의 등록 및 수정
     */
    @PostMapping(value = "/registerRequestCont", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerRequestCont(@RequestBody Map<String, Object> param) {
        try {
            return adminService.registerRequestCont(param);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> rtnMap = new HashMap<String, Object>();
            rtnMap.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return ResponseEntity.internalServerError().body(rtnMap);
        }
    }
}
