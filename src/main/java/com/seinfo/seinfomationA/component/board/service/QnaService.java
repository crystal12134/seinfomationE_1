package com.seinfo.seinfomationA.component.board.service;

import com.seinfo.seinfomationA.component.board.Model.QnaModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public interface QnaService {

    ModelAndView loadQnaPage(int page, String keyword);

    ResponseEntity<?> reloadQnaPage(Map<String, Object> param);

    ResponseEntity<?> registQuestion(QnaModel qnaModel);

    ResponseEntity<?> registReply(QnaModel qnaModel);

    ResponseEntity<?> getQuestionDetail(Map<String, Object> param);
}
