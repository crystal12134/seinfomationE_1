package com.seinfo.seinfomationA.component.request.service;

import com.seinfo.seinfomationA.component.request.model.RequestModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

public interface RequestService {

    ModelAndView loadRequestRegFormPage(Map<String, Object> param);

    ModelAndView loadRequestListPage(int page);

    ModelAndView loadRequestDetailPage(Map<String, Object> param);

    ResponseEntity<?> reloadRequestDetail(Map<String, Object> param);

    ResponseEntity<?> registerRequest(RequestModel requestModel) throws Exception;

    ResponseEntity<?> removeRequest(Map<String, Object> param) throws Exception;

    ResponseEntity<?> getRequestCont(Map<String, Object> param);

    ResponseEntity<?> registerRequestCont(Map<String, Object> param) throws Exception; // 질문 및 답변 등록
}
