package com.seinfo.seinfomationA.component.admin.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public interface AdminService {

    ModelAndView getRequestList(int page);

    ResponseEntity<?> reloadRequestList(int page);

    ModelAndView getRequestDetail(Map<String, Object> param);

    ResponseEntity<?> reloadRequestDetail(Map<String, Object> param);

    ResponseEntity<?> getRequestCont(Map<String, Object> param);

    ResponseEntity<?> updateRequestState(Map<String, Object> param) throws Exception;

    ResponseEntity<?> registerRequestCont(Map<String, Object> param) throws Exception; // 질문 및 답변 등록

}
