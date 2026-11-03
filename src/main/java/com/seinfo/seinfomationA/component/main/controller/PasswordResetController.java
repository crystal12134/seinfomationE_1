package com.seinfo.seinfomationA.component.main.controller;
import com.seinfo.seinfomationA.component.main.service.impl.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Controller @RequiredArgsConstructor
@RequestMapping("/login/passwordReset")
public class PasswordResetController {
    private final PasswordResetService service;
    public record ResetRequest(String token, String newPassword) {}
    @GetMapping
    public ModelAndView page(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        response.setHeader("Referrer-Policy","no-referrer");
        return new ModelAndView("seinfo/main/passwordReset");
    }
    @PostMapping("/complete") @ResponseBody
    public ResponseEntity<?> complete(@RequestBody ResetRequest input) {
        try {
            service.complete(input.token(), input.newPassword());
            return ResponseEntity.ok().header("Cache-Control","no-store")
                .body(Map.of("message","비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().header("Cache-Control","no-store").body(Map.of("message",e.getMessage()));
        }
    }
}
