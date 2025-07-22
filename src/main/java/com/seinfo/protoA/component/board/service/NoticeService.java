package com.seinfo.protoA.component.board.service;

import com.seinfo.protoA.component.board.Model.NoticeModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public interface NoticeService {

    ModelAndView loadNoticeBoardPage(int page, String keyword, String searchType, String noticeNo);

    ResponseEntity<?> reloadNoticeList(Map<String, Object> param);

    ResponseEntity<?> registNotice(NoticeModel noticeModel);

    ResponseEntity<?> getNoticeDetail(Map<String, Object> param);

    ResponseEntity<?> deleteNotice(Map<String, Object> param) throws Exception;

}
