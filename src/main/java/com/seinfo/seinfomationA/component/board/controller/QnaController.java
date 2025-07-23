package com.seinfo.seinfomationA.component.board.controller;

import com.seinfo.seinfomationA.component.board.Model.QnaModel;
import com.seinfo.seinfomationA.component.board.service.QnaService;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
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

    /**
     * 페이지 호출 : QnA (외부:LRTP)
     */
    @GetMapping(value = "/qnaList")
    public ModelAndView qnaListPage(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(required = false) String keyword) {
        return qnaService.loadQnaPage(page, keyword);
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
}
