package com.seinfo.protoA.util.api;

import com.seinfo.protoA.util.EncriptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiServiceImpl implements ApiService {

    private final AipMapper aipMapper;

    @Override
    public ResponseEntity<?> getCodeList(Map<String, Object> param) {
        return ResponseEntity.ok(Map.of("codeList", aipMapper.getCodeList(param)));
    }

    @Override
    public ResponseEntity<?> getEncryptParam(Map<String, Object> param) throws Exception {
        String paramData = Objects.toString(param.get("paramData"), "");
        if(paramData.isBlank()) {
            throw new RuntimeException("파라미터 데이터가 없습니다.");
        }
        String encryptParam = EncriptUtil.encryptAes256(paramData);
        return ResponseEntity.ok(Map.of("encData", encryptParam));
    }
}
