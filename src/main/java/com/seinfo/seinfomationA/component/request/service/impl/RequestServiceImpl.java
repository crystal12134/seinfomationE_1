package com.seinfo.seinfomationA.component.request.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seinfo.seinfomationA.component.file.mapper.TusMapper;
import com.seinfo.seinfomationA.component.file.service.TusService;
import com.seinfo.seinfomationA.component.request.mapper.RequestMapper;
import com.seinfo.seinfomationA.component.request.model.RequestModel;
import com.seinfo.seinfomationA.component.request.service.RequestService;
import com.seinfo.seinfomationA.util.ComUtil;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.CoreWebApiResponseModel;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.security.AuthUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.extras.springsecurity6.auth.AuthUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final TokenUtils tokenUtils;
    private final RequestMapper requestMapper;;
    private final ResponseEntityUtil responseEntityUtil;
    private final TusMapper tusMapper;
    private final TusService tusService;
    private final AuthUtil authUtil;

    /**
     * 페이지호출 : 제안서작성
     */
    @Override
    public ModelAndView loadRequestRegFormPage(Map<String, Object> param) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/request/requestRegForm");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            mv.addObject("message", "로그인이 필요합니다.");
            return mv;
        }

        String userEmail = authentication.getName();
        param.put("userEmail", userEmail);
        Map<String, Object> userInfo = requestMapper.selectRequestUserInfo(param);
        Map<String, Object> requestInfo = requestMapper.selectRequestRegForm(param);
        mv.addObject("userInfo", userInfo);
        mv.addObject("requestInfo", requestInfo);
        mv.addObject("detailYn", Objects.toString(param.get("detailYn"), "N"));
        mv.addObject("page", Objects.toString(param.get("page"), ""));

        return mv;
    }

    /**
     * 페이지호출 : 제안이력조회
     */
    @Override
    public ModelAndView loadRequestListPage(int page) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/request/requestList");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            mv.addObject("message", "로그인이 필요합니다.");
            return mv;
        }

        Map<String, Object> param = new HashMap<>();

        // 전체 레코드 수 조회
        param.put("userEmail", authentication.getName());
        int totalRecords = requestMapper.getRequestListCount(param);

        // 페이지 정보 계산
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); // 최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); // 현재 페이지 범위 제한
        int startRow = (cPage - 1) * pageSize;

        // 리스트 조회
        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> requestList = requestMapper.selectRequestList(param);

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        mv.addObject("requestList", requestList);
        mv.addObject("pageResult", pageResult);

        return mv;
    }

    /**
     * 페이지호출 : 제안이력상세
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView loadRequestDetailPage(Map<String, Object> param) {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/request/requestDetail");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            mv.addObject("message", "로그인이 필요합니다.");
            return mv;
        }

        param.put("userEmail", authentication.getName());
        Map<String, Object> processMap = requestMapper.selectRequestProcess(param);
        List<Map<String, Object>> requestDetailList = requestMapper.selectRequestDetail(param);

        mv.addObject("requestDetailList", requestDetailList);
        mv.addObject("processMap", processMap);
        mv.addObject("page", Objects.toString(param.get("page"), ""));

        return mv;
    }

    @Override
    public ResponseEntity<?> reloadRequestDetail(Map<String, Object> param) {
        param.put("userEmail", SecurityContextHolder.getContext().getAuthentication().getName());

        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> processMap = requestMapper.selectRequestProcess(param);
        List<Map<String, Object>> requestDetailList = requestMapper.selectRequestDetail(param);

        resultMap.put("processMap", processMap);
        resultMap.put("requestDetailList", requestDetailList);

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", resultMap);
    }

    /**
     * 저장 : 제안서작성
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    @Transactional
    public ResponseEntity<?> registerRequest(RequestModel requestModel) throws Exception {
        Map<String, Object> param = new ObjectMapper().convertValue(requestModel, Map.class);
        Map<String, Object> rtnResult = new HashMap<String, Object>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // usr정보 조회
        Map<String, Object> usrResultMap = requestMapper.selectUserUid(Map.of("userEmail", userEmail));
        param.put("userEmail", userEmail);
        param.put("userUid", usrResultMap.get("userUid").toString());
        param.put("blYn", usrResultMap.get("blYn").toString());

        // 첨부파일 그룹ID 체크
        String mfileGrpId = String.valueOf(param.get("fileGrpId"));
        try {
            if (!mfileGrpId.isEmpty()) {
                if (tusMapper.selectMfileListCnt(Map.of("mfileGrpId", mfileGrpId)) == 0) {
                    param.remove("fileGrpId");
                }
            }
        } catch (Exception e) {
            rtnResult.put("message", e.getMessage());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", rtnResult);
        }

        // requestNo 처리
        String requestNo = Objects.toString(param.get("requestNo"), "");
        if (requestNo.isBlank()) { // 신규등록시
            Map<String, Object> proposalMap = requestMapper.getLastRequestNo();
            String lastRequestNo = "";
            if (proposalMap != null) {
                lastRequestNo = proposalMap.get("proposalNo").toString();
            }

            int nextProposalNo = 1;
            if (!lastRequestNo.isBlank()) {
                String lastRequestStr = lastRequestNo.substring(lastRequestNo.length() - 3);
                nextProposalNo = Integer.parseInt(lastRequestStr) + 1;
            }

            param.put("requestNo", "REQ" + DateFormatUtils.format(new Date(), "yyMM") + String.format("%03d", nextProposalNo));
        }

        if (Objects.equals("10", param.get("requestState"))) {
            param.put("sendD", DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
        }

        // 1. 의뢰작성
        if (!requestMapper.insertRequest(param)) {
            throw new RuntimeException("의뢰작성 - 저장 중 서버내 오류가 발생하였습니다.");
        }

        rtnResult.put("saveRequestNo", param.get("requestNo"));
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnResult);
    }

    /**
     * 삭제 : 제안서작성
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<?> removeRequest(Map<String, Object> param) throws Exception {
        Map<String, Object> rtnResult = new HashMap<String, Object>();
        param.put("userUid", this.tokenUtils.getClaimsByValue("USER_EMP_NO"));

        // 첨부파일 key 조회
        Map<String, Object> mfileGrpIdMap = requestMapper.selectRequestFileGrpId(param);

        try {
            // 1. request 데이터 삭제 (진행단계 : 작성중 일때만, 삭제가능)
            int dCnt = requestMapper.deleteRequest(param);
            if (dCnt == 0) {
                String msg = "삭제 중 서버 내 오류가 발생하였습니다.";
                log.error("removeRequest >>> deleteProposal : {}", msg);
                throw new RuntimeException(msg);
            }

            // 2. 물리적 파일 및 DB 삭제
            if (mfileGrpIdMap != null) {  // mfileGrpId 존재시
                List<Map<String, Object>> dataList = tusMapper.selectMfileListClient(mfileGrpIdMap);
                //log.info("removeProposal >>> selectMFileListClient >>> dataList.size() : "+ dataList.size());
                if (!dataList.isEmpty()) { // 파일리스트 존재시
                    dataList.forEach(list -> {
                        list.put("file_grp_id", mfileGrpIdMap.get("mfileGrpId"));
                    });

                    ResponseEntity<?> rtn = tusService.deleteFile(dataList);
                    // 응답에서 process_code 및 data 추출
                    CoreWebApiResponseModel resultModel = (CoreWebApiResponseModel) rtn.getBody();
                    String processCode = resultModel.getProcess_code();

                    if (!"00".equals(processCode)) {
                        // data 필드는 Object이므로, Map으로 변환 가능한 경우에만 캐스팅
                        Object dataObj = resultModel.getData();
                        if (dataObj instanceof Map) {
                            Map<String, Object> data = (Map<String, Object>) dataObj;
                            String errorMsg = (String) data.get("message");

                            String msg = "삭제 중 서버 내 오류가 발생하였습니다. (파일삭제) <br> ▷ " + errorMsg;
                            log.error("removeRequest >>> deleteFile >>> message : " + msg);
                            throw new RuntimeException(msg);
                        } else {
                            log.error("removeRequest >>> deleteFile >>> data 타입이 Map이 아님: {}", dataObj.getClass());
                            throw new RuntimeException("삭제 중 서버 내 오류가 발생하였습니다. (파일삭제: data 형식 오류)");
                        }
                    }
                }
            }
        } catch (DataAccessException e) {
            log.error("removeRequest>>> DataAccessException : {}", e.toString());
            throw new Exception("예외발생 : 처리할 수 없습니다. (DataAccessException)");
        } catch (Exception e) {
            log.error("removeRequest >>> Exception : {}", e.toString());
            throw new Exception(e.getMessage());
        }

        rtnResult.put("success", "success");
        return ResponseEntity.ok(rtnResult);
    }

    /**
     * 추가 의뢰 질문&답변 조회
     */
    @Override
    public ResponseEntity<?> getRequestCont(Map<String, Object> param) {

        param.put("userRole", SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString());
        Map<String, Object> requestCont = requestMapper.selectRequestCont(param);

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", requestCont);
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
            errorResult.put("message", e.getMessage());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        param.put("userEmail", authentication.getName());
        if(Objects.equals("N", param.get("replyYn"))){
            int requestSeq = requestMapper.getNextRequestSeq(param);
            param.put("requestSeq", requestSeq);
        }

        try {
            requestMapper.insertRequestCont(param);
        }catch(Exception e){
            log.error("registerRequestCont >>> Exception : {}", e.toString());
            throw new RuntimeException("저장 중 오류가 발생하였습니다.");
        }

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "저장되었습니다.");
    }
}