package com.seinfo.protoA.util.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final ApiService apiService;

    @PostMapping("/getCodeList")
    public ResponseEntity<?> getCodeList(@RequestBody Map<String, Object> param) {
        return apiService.getCodeList(param);
    }

    @PostMapping(value = "/getEncryptParam", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getEncryptParam(@RequestBody Map<String, Object> param) {
        try {
            return apiService.getEncryptParam(param);
        }catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }catch (Exception e) {
            log.error("Exception : {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "서버내 오류 발생"));
        }
    }
}
