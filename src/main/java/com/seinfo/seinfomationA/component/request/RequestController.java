package com.seinfo.seinfomationA.component.request;

import com.seinfo.seinfomationA.component.request.model.RequestModel;
import com.seinfo.seinfomationA.component.request.service.RequestService;
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

    /**
     * 페이지호출: 제안서 작성
     */
    @GetMapping(value = "/requestRegForm", produces = MediaType.TEXT_XML_VALUE)
    public ModelAndView ppsRegFormPage(@RequestParam(required = false) String paramData) {
        Map<String, Object> param = fnDecParam(paramData);
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
        Map<String, Object> param = fnDecParam(paramData);
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
            log.error("registerRequest   >>> Exception >>> 예외발생:{}", e.toString());
            errorResult.put("message", e.getMessage());
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
            Map<String, Object> rtnMap = new HashMap<String, Object>();
            rtnMap.put("message", e.getMessage());
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
            Map<String, Object> rtnMap = new HashMap<String, Object>();
            rtnMap.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(rtnMap);
        }
    }

    /**
     * 복호화
     */
    private Map<String, Object> fnDecParam(String paramData) {
        Map<String, Object> param = new HashMap<>();
        if (paramData != null && !paramData.isBlank()) {
            try {
                String decParamData = EncriptUtil.decryptAes256(paramData);

                if (!decParamData.isBlank()) {
                    String[] params = decParamData.split("&");
                    param = Arrays.stream(params)
                            .map(p -> p.split("=", 2))
                            .filter(p -> p.length == 2)
                            .collect(Collectors.toMap(p -> p[0], p -> p[1]));
                }
            } catch (Exception e) {
                log.warn("파라미터 복호화 실패 or 파싱 실패: {}", e.getMessage());
            }
        }
        return param;
    }
}
