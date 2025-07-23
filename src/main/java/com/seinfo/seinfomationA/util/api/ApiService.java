package com.seinfo.seinfomationA.util.api;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface ApiService {
    ResponseEntity<?> getCodeList(Map<String, Object> param);
    ResponseEntity<?> getEncryptParam(Map<String, Object> param) throws Exception;
}
