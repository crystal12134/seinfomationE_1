package com.seinfo.seinfomationA.component.file.controller;

import com.seinfo.seinfomationA.component.file.service.TusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(exposedHeaders =  { "Location", "Upload-Offset"})
@RequiredArgsConstructor
public class TusController {

    private final TusService tusService;

    // 업로드 엔드포인트
    @RequestMapping(value = { "/upload", "/upload/**"}, method = { RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.DELETE, RequestMethod.GET})
    public ResponseEntity<?> tusUpload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return ResponseEntity.ok(tusService.tusUpload(request, response));
    }

    // 첨부파일 설정 값 가져오기
    @PostMapping(value = "/tus/mfileInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMfileInfo(@RequestBody Map<String, Object> data) throws Exception {
        return tusService.getMfileInfo(data);
    }

    // 첨부파일 목록 가져오기
    @PostMapping(value = "/tus/mfileGroupInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMfileGroupInfo(@RequestBody Map<String, Object> data) throws Exception {
        return tusService.getMfileGroupInfo(data);
    }

    // 첨부파일 다운 권한 확인
    @PostMapping(value = "/tus/mfilePermit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mfilePermit(@RequestBody Map<String, Object> data) throws Exception {
        return tusService.getMfileDownInfo(data);
    }

    // 첨부파일 삭제
    @PostMapping(value = "/tus/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFile(@RequestBody List<Map<String, Object>> data) throws Exception {
        return tusService.deleteFile(data);
    }

    // 비공개 파일 다운로드 API
    @GetMapping(value = "/private/filedownload")
    public ResponseEntity<?> getPrivateFileDown(HttpServletResponse response, @RequestParam String mfileDownId) throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put("mfileDownId", mfileDownId);
        return tusService.getFileDown(response, param);
    }
}
