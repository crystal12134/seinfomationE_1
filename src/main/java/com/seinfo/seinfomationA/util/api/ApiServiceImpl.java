package com.seinfo.seinfomationA.util.api;

import com.seinfo.seinfomationA.util.EncriptUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;


import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiServiceImpl implements ApiService {

    @Value("${server.domain}")
    private String domain;

    @Value("${spring.mail.username:}")
    private String mailSender;

    private final ApiMapper apiMapper;
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    public ResponseEntity<?> getCodeList(Map<String, Object> param) {
        return ResponseEntity.ok(Map.of("codeList", apiMapper.getCodeList(param)));
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

    @Async("mailExecutor")
    public void sendMail(Map<String, Object> mailContent, String setViewName) {
        try {
            templateMailSend(mailContent, setViewName); // 기존 동기 메서드 재사용
        } catch (Exception e) {
            // 후처리 안 할 거면 로그만 남기고 끝
            log.error("Async mail send failed: {}", e.getMessage(), e);
        }
    }

    /**
     * @param mailContent
     * @param setViewName(템블릿위치)
     * @throws Exception
     */
    @Override
    public void templateMailSend(Map<String, Object> mailContent, String setViewName) throws Exception {
        try {
            mailContent.put("mailUrl", domain);

            Context ctx = new Context(Locale.KOREA);
            mailContent.forEach(ctx::setVariable);
            String html = templateEngine.process(setViewName, ctx);

            // 2) 메시지 생성
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.addInline("logoImage", new ClassPathResource("static/seinfo/img/logo.png"), "image/png");
            //helper.setTo("blbl9857@naver.com");
            helper.setTo(mailContent.get("userEmail").toString());
            helper.setSubject(mailContent.get("mailTitle").toString());
            helper.setFrom(mailSender, "SE정보기술");
            helper.setText(html, true);

            javaMailSender.send(message);
        }catch(Exception e) {
            log.error("fnSendMail : {}", e.getMessage());
            throw e;
        }
    }
}
