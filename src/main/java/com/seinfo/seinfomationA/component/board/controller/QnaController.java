package com.seinfo.seinfomationA.component.board.controller;

import com.seinfo.seinfomationA.component.board.Model.QnaModel;
import com.seinfo.seinfomationA.component.board.service.QnaService;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class QnaController {

    private final QnaService qnaService;
    private final ResponseEntityUtil responseEntityUtil;
    private final TokenUtils tokenUtils;

    /**
     * 페이지 호출 : QnA
     */
    @GetMapping(value = "/qnaList")
    public ModelAndView qnaListPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "member") String qnaMode) {
        return qnaService.loadQnaPage(page, keyword, qnaMode);
    }

    /**
     * QnA 목록 재조회
     */
    @PostMapping(value = "/qnaList/reloadQnaList")
    public ResponseEntity<?> getqnaList(@RequestBody Map<String, Object> param) {
        //토큰 유효성 체크
        Map<String, String> errorResult = new HashMap<>();
        try{
            String username = (String) tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID);
        }catch (ExpiredJwtException e) {
            log.error("getqnaList >>> ExpiredJwtException >>> 토큰 유효성 체크 : {}", e.toString());
            errorResult.put("message", "토큰 유효성 체크 : 처리할 수 없습니다. (ExpiredJwtException)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("getqnaList >>> Exception >>> 토큰 유효성 체크 : {}", e.toString());
            errorResult.put("message", "토큰 유효성 체크 : 처리할 수 없습니다. (Exception)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
        return qnaService.reloadQnaPage(param);
    }

    /**
     * QnA 입력 (문의하기) : 팝업화면 (외부:LRTP)
     */
    @PostMapping(value = "/qnaList/registQuestion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registQuestion(@RequestBody QnaModel qnaModel, BindingResult bindingResult) {
        Map<String, Object> errorResult = new HashMap<>();
        if(bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if(fieldError != null) {
                String errorMessage = fieldError.getDefaultMessage();
                errorResult.put("message", "fieldError::" + errorMessage);
            }
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
        return qnaService.registQuestion(qnaModel);
    }

    /**
     * QnA 입력(답변하기) : 팝업화면 (내부:RTP)
     */
    @PostMapping(value = "/qnaList/registReply", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registReply(@RequestBody QnaModel qnaModel) {
        Map<String, String> errorResult = new HashMap<>();
        try {
            String username = (String)tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID);
        }catch (ExpiredJwtException e) {
            log.error("registReply >>> ExpiredJwtException >>> 토큰 유효성 체크 : {}", e.toString());
            errorResult.put("message", "토큰 유효성 체크 : 처리할 수 없습니다. (ExpiredJwtException)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("registReply >>> Exception >>> 토큰 유효성 체크 : {}", e.toString());
            errorResult.put("message", "토큰 유효성 체크 : 처리할 수 없습니다. (Exception)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
        return qnaService.registReply(qnaModel);
    }

    /**
     * QnA 상세조회 (문의하기), (답변하기) : 팝업화면
     */
    @PostMapping(value = "/qnaList/getQnaDetail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getQuestionDetail(@RequestBody Map<String, Object> param) {
        //토큰 유효성 체크
        Map<String, String> errorResult = new HashMap<>();
        try {
            String username = (String) tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID);
        } catch (ExpiredJwtException e) {
            log.error("getQuestionDetail >>> ExpiredJwtException >>> 토큰 유효성 체크 : {}", e.toString());
            errorResult.put("message", "토큰 유효성 체크 : 처리할 수 없습니다. (ExpiredJwtException)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("getQuestionDetail >>> Exception >>> 토큰 유효성 체크 : {}", e.toString());
            errorResult.put("message", "토큰 유효성 체크 : 처리할 수 없습니다. (Exception)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
        return qnaService.getQuestionDetail(param);
    }
}


