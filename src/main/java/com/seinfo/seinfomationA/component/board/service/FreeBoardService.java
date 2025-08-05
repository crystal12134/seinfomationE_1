package com.seinfo.seinfomationA.component.board.service;

import com.seinfo.seinfomationA.component.board.Model.FreeBoardModel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public interface FreeBoardService {

    ModelAndView loadFreeBoardBoardPage(int page, String keyword, String searchType, String freeBoardNo);

    ResponseEntity<?> reloadFreeBoardList(Map<String, Object> param);

    ResponseEntity<?> registFreeBoard(FreeBoardModel freeBoardModel);

    ResponseEntity<?> getFreeBoardDetail(Map<String, Object> param);

    ResponseEntity<?> deleteFreeBoard(Map<String, Object> param) throws Exception;

}
