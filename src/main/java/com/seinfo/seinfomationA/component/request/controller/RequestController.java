package com.seinfo.seinfomationA.component.request.controller;

import com.seinfo.seinfomationA.component.request.model.RequestModel;
import com.seinfo.seinfomationA.component.request.service.RequestService;
import com.seinfo.seinfomationA.util.ComUtil;
import com.seinfo.seinfomationA.util.EncriptUtil;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/request")
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final RequestService requestService;
    private final ResponseEntityUtil responseEntityUtil;
    private final ComUtil comUtil;

    /**
     * 페이지호출: 제안서 작성
     */
    @GetMapping(value = "/requestRegForm", produces = MediaType.TEXT_XML_VALUE)
    public ModelAndView ppsRegFormPage(@RequestParam(required = false) String paramData) {
        Map<String, Object> param = comUtil.fnDecParam(paramData);
        return requestService.loadRequestRegFormPage(param);
    }

    /**
     * 페이지 호출: 제안이력 조회**/
    @GetMapping(value = "/requestList")
    public ModelAndView ppsHistoryListPage(@RequestParam(defaultValue = "1") int page) {
        return requestService.loadRequestListPage(page);
    }

    /**
     * 페이지 호출: 제안 이력 상세**/
    @GetMapping(value = "/requestDetail", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView ppsHistoryMainPage(@RequestParam(required = false) String paramData){
        Map<String, Object> param = comUtil.fnDecParam(paramData);
        return requestService.loadRequestDetailPage(param);
    }

    /**
     * 저장: 제안서 작성
     */

    @PostMapping(value = "/registerRequest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processProposal(@RequestBody RequestModel requestModel, BindingResult bindingResult) {
        Map<String, Object> errorResult = new HashMap<String, Object>();

        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                String errorMessage = fieldError.getDefaultMessage();
                errorResult.put("message", errorMessage);
            }
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        try {
            return requestService.registerRequest(requestModel);
        } catch(DataAccessException e) {
            log.error("registerRequest   >>> DataAccessException >>> 예외발생:{}", e.toString());
            errorResult.put("message", "예외발생    :처리할 수 없습니다. (DataAccessException)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("registerRequest   >>> Exception >>> 예외발생:{}", e.getMessage());
            log.error("요청 처리 중 오류가 발생했습니다.");
            errorResult.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
    }

    /**
     * 삭제 : 제안서작성
     */
    @DeleteMapping(value = "/removeRequest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeProposal(@RequestBody Map<String, Object> param) {
        try {
            return requestService.removeRequest(param);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> rtnMap = new HashMap<String, Object>();
            rtnMap.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return ResponseEntity.internalServerError().body(rtnMap);
        }
    }

    /**
     * requestDetail 재조회
     */
    @PostMapping(value = "/reloadRequestDetail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reloadRequestDetail(@RequestBody Map<String, Object> param) {
        return requestService.reloadRequestDetail(param);
    }

    /**
     * 추가문의 불러오기
     */
    @PostMapping(value = "/getRequestCont", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRequestCont(@RequestBody Map<String, Object> param) {
        return requestService.getRequestCont(param);
    }

    /**
     * 추가문의 등록 및 수정
     */
    @PostMapping(value = "/registerRequestCont", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerRequestCont(@RequestBody Map<String, Object> param) {
        try {
            return requestService.registerRequestCont(param);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> rtnMap = new HashMap<String, Object>();
            rtnMap.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", rtnMap);
        }
    }
}
