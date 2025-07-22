package com.seinfo.protoA.component.board.controller;

import com.seinfo.protoA.component.board.Model.FaqModel;
import com.seinfo.protoA.component.board.service.FaqService;
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
@RequestMapping("/board/")
@RequiredArgsConstructor
@Slf4j
public class FaqController {

    private final FaqService faqService;

    /**
     * 페이지 호출 : FAQ
     */
    @GetMapping(value = "/faqList")
    public ModelAndView loadFaqListPage(@RequestParam(required = false) Integer faqNo,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "all") String faqType) {
    return faqService.loadFaqListPage(faqNo, page, faqType);
    }

    /**
     * FAQ 글 등록
     */
    @PostMapping(value = "/registerFaq", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerFaq(@RequestBody FaqModel faqModel, BindingResult bindingResult) {

        Map<String, Object> message = new HashMap<String, Object>();

        if(bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();

            if(fieldError != null) {
                String errorMessage = fieldError.getDefaultMessage();

                message.put("message", errorMessage);
            }
            return ResponseEntity.badRequest().body(message);
        }
        return faqService.registFaq(faqModel);
    }

    /**
     * FAQ 수정 (조회건수 증가)
     */
    @PutMapping(value = "/updateInquireCnt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateInquireCnt(@RequestBody Map<String, Object> param){
        return faqService.updateInquireCnt(param);
    }
}
