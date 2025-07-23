package com.seinfo.seinfomationA.component.board.service;

import com.seinfo.seinfomationA.component.board.Model.FaqModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public interface FaqService {

    ModelAndView loadFaqListPage(Integer faqNo, int page, String faqType);

    ResponseEntity<?> registFaq(FaqModel faqModel);

    ResponseEntity<?> updateInquireCnt(Map<String, Object> param);
}
