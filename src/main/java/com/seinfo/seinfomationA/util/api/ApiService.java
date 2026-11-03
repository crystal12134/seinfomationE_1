package com.seinfo.seinfomationA.util.api;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface ApiService {
    ResponseEntity<?> getCodeList(Map<String, Object> param);
    ResponseEntity<?> getEncryptParam(Map<String, Object> param) throws Exception;
    void templateMailSend(Map<String, Object> mailParam, String setViewName) throws Exception;
    void sendMail(Map<String, Object> mailMap, String setViewName);
}
