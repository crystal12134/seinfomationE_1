package com.seinfo.seinfomationA.component.admin.service.impl;

import com.seinfo.seinfomationA.component.admin.mapper.AdminMapper;
import com.seinfo.seinfomationA.component.admin.service.AdminService;
import com.seinfo.seinfomationA.component.file.mapper.TusMapper;
import com.seinfo.seinfomationA.util.api.ApiMapper;
import com.seinfo.seinfomationA.util.api.ApiService;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.security.AuthUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final ApiMapper apiMapper;
    private final AuthUtil authUtil;
    private final ResponseEntityUtil responseEntityUtil;
    private final TokenUtils tokenUtils;
    private final TusMapper tusMapper;
    private final ApiService apiService;

    @Override
    public ModelAndView getRequestList(int page) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/admin/requestList");

        if (!authUtil.isRoleInTokenAndAuth("ADMIN")) {
            mv.addObject("message", "권한이 불충분 합니다.");
            return mv;
        }

        // 전체 레코드 수 조회
        int totalRecords = adminMapper.getRequestListCountForAdmin();

        // 페이지 정보 계산
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); // 최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); // 현재 페이지 범위 제한
        int startRow = (cPage - 1) * pageSize;

        // 리스트 조회
        Map<String, Object> param = new HashMap<>();
        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> requestList = adminMapper.selectRequestListForAdmin(param);

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        mv.addObject("requestList", requestList);
        mv.addObject("pageResult", pageResult);

        return mv;
    }

    @Override
    public ResponseEntity<?> reloadRequestList(int page){
        // 전체 레코드 수 조회
        int totalRecords = adminMapper.getRequestListCountForAdmin();

        // 페이지 정보 계산
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); // 최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); // 현재 페이지 범위 제한
        int startRow = (cPage - 1) * pageSize;

        // 리스트 조회
        Map<String, Object> param = new HashMap<>();
        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> requestList = adminMapper.selectRequestListForAdmin(param);

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        Map<String, Object> rtnMap = new HashMap<>();
        rtnMap.put("requestList", requestList);
        rtnMap.put("pageResult", pageResult);

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnMap);
    }

    @Override
    public ModelAndView getRequestDetail(Map<String, Object> param) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/admin/requestDetail");
        if (!authUtil.isRoleInTokenAndAuth("ADMIN")) {
            mv.addObject("message", "권한이 불충분 합니다.");
            return mv;
        }

        param.put("userEmail", SecurityContextHolder.getContext().getAuthentication().getName());
        Map<String, Object> requestDetailMap = adminMapper.selectRequestDetail(param);
        List<Map<String, Object>> requestContList = adminMapper.selectRequestContList(param);

        mv.addObject("page", param.get("page"));
        mv.addObject("requestDetail", requestDetailMap);
        mv.addObject("requestContList", requestContList);

        return mv;
    }

    @Override
    public ResponseEntity<?> reloadRequestDetail(Map<String, Object> param) {
        param.put("userEmail", SecurityContextHolder.getContext().getAuthentication().getName());
        Map<String, Object> requestDetailMap = adminMapper.selectRequestDetail(param);
        List<Map<String, Object>> requestContList = adminMapper.selectRequestContList(param);

        Map<String, Object> rtnMap = new HashMap<>();
        rtnMap.put("requestDetail", requestDetailMap);
        rtnMap.put("requestContList", requestContList);

        return responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnMap);
    }

    /**
     * 추가 의뢰 질문&답변 조회
     */
    @Override
    public ResponseEntity<?> getRequestCont(Map<String, Object> param) {

        param.put("userRole", SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString());
        Map<String, Object> requestCont = adminMapper.selectRequestContDetail(param);

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", requestCont);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateRequestState(Map<String, Object> param) throws Exception {
        if (!authUtil.isRoleInTokenAndAuth("ADMIN")) {
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001",  "변경 실패");
        }

        Map<String, Object> mailMap = apiMapper.getUserEmailInfo(param);
        mailMap.put("mailTitle", "요청하신 의뢰가 접수되었습니다.");
        String requestState = Objects.toString(param.get("requestState"), "");
        if(requestState.equals("10")){
            param.replace("requestState", "20");
            apiService.sendMail(mailMap,"seinfo/mail/mailTemplate01");
        }else if(requestState.equals("20")){
            param.replace("requestState", "99");
        }

        param.put("userEmail", tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID));
        adminMapper.updateRequestState(param);


        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "S001");
    }

    /**
     * 추가 의뢰 질문&답변 등록
     */
    @Override
    public ResponseEntity<?> registerRequestCont(Map<String, Object> param) throws Exception {
        Map<String, Object> errorResult = new HashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            errorResult.put("message", "로그인이 필요합니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        String mfileGrpId = Objects.toString(param.get("mfileGrpId"), "");
        try {
            if (!mfileGrpId.isBlank()) {
                if (tusMapper.selectMfileListCnt(Map.of("mfileGrpId", mfileGrpId)) == 0) {
                    param.remove("mfileGrpId");
                }
            }
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            errorResult.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }


        param.put("userEmail", authentication.getName());
        Map<String, Object> mailMap = apiMapper.getUserEmailInfo(param);
        if(Objects.equals("N", param.get("replyYn"))){
            int requestSeq = adminMapper.getNextRequestSeq(param);
            param.replace("requestSeq", requestSeq);
            mailMap.put("mailTitle", "관리자가 해당 의뢰에 대한 질문 또는 요청을 등록하였습니다.");
            mailMap.put("mailContent", "관리자가 해당 의뢰에 대한 질문 또는 요청을 등록하였습니다.");
        }else {
            mailMap.put("mailTitle", "관리자가 질문 또는 요청에대한 답변을 등록하였습니다.");
            mailMap.put("mailContent", "관리자가 질문 또는 요청에대한 답변을 등록하였습니다.");
        }
        apiService.sendMail(mailMap,"seinfo/mail/mailTemplate02");

        try {
            adminMapper.insertRequestCont(param);
        }catch(Exception e){
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("registerRequestCont >>> Exception : {}", e.toString());
            throw new RuntimeException("저장 중 오류가 발생하였습니다.");
        }

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "저장되었습니다.");
    }
}
