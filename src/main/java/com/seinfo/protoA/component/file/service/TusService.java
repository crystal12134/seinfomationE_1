package com.seinfo.protoA.component.file.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import me.desair.tus.server.exception.TusException;
import org.springframework.http.ResponseEntity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public interface TusService {

    String tusUpload(HttpServletRequest request, HttpServletResponse response) throws IOException, TusException, Exception;

    ResponseEntity<?> getFileDown(HttpServletResponse response, Map<String, Object> param) throws Exception;

    ResponseEntity<?> getMfileInfo(Map<String, Object> data) throws Exception;

    ResponseEntity<?> getMfileGroupInfo(Map<String, Object> data) throws Exception;

    ResponseEntity<?> getMfileDownInfo(Map<String, Object> data) throws Exception;

    ResponseEntity<?> deleteFile(List<Map<String, Object>> data) throws Exception;
}
