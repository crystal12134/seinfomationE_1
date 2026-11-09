package com.seinfo.seinfomationA.util.security;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
@RestControllerAdvice
public class SecurityErrorAdvice {
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<?> invalidToken(){return ResponseEntity.status(401).body(Map.of("message","다시 로그인해주세요."));}
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> denied(){return ResponseEntity.status(403).body(Map.of("message","해당 자료에 접근할 권한이 없습니다."));}
    @ExceptionHandler({IllegalArgumentException.class,java.io.IOException.class})
    public ResponseEntity<?> invalid(){return ResponseEntity.badRequest().body(Map.of("message","요청 정보와 파일 형식을 확인해주세요."));}
}
