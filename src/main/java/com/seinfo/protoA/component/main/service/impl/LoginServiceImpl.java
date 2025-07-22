package com.seinfo.protoA.component.main.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.seinfo.protoA.component.member.mapper.MemberMapper;
import com.seinfo.protoA.util.response.CoreWebApiUserDetail;
import com.seinfo.protoA.util.token.TokenMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
import com.seinfo.protoA.util.token.TokenUtils;
import com.seinfo.protoA.component.main.mapper.LoginMapper;
import com.seinfo.protoA.component.main.model.LoginModel;
import com.seinfo.protoA.component.main.service.LoginService;
import com.seinfo.protoA.util.CookieUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final HttpServletResponse res;
    private final HttpServletRequest req;
    private final AuthenticationManager authenticationManager;
    private final LoginMapper loginMapper;
    private final TokenMapper tokenMapper;
    private final ResponseEntityUtil responseEntityUtil;
    private final TokenUtils tokenUtils;
    private final JavaMailSender javaMailSender;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Value("${server.type:local}")
    private String serverType;

    @Value("${service.dup.ipcheck:false")
    private String checkDupIP;
    /**
     * 로그아웃
     *
     * @throws Exception
     */
    @Override
    public ResponseEntity<?> removeToken() throws Exception {

        String beforeToken = tokenUtils.getTokenFromCookie();

        //쿠키 삭제
        new CookieUtil().removeCookie(TokenUtils.TOKEN_KEY_HEADER, res, serverType);

        //중복 로그인 차단 사용시
        if (beforeToken != null &&"true".equals(checkDupIP)) {

            Object userEmail = tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID);

            // 토큰 사용 차단
            String sb = "web logout process ";
            Map<String,Object> aliveToken = new HashMap<>();

            aliveToken.put("tokenUserId", userEmail);
            aliveToken.put("tokenKey", beforeToken);
            aliveToken.put("tokenDeniedDesc", sb);
            tokenMapper.insertTokenBanned(aliveToken);
         }

        return responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "정상적으로 처리 되었습니다.", null);
    }

    /**
     * 로그인 프로세스
     *
     * @throws Exception
     */
    @Override
    @Transactional
    public ResponseEntity<?> loginProcess(LoginModel loginModel) throws  Exception {
        Map<String, Object> errorResult = new HashMap<>();

        Map<String, Object> param = new HashMap<>();
        String userEmail  = loginModel.getId();
        String userPwd = loginModel.getPassword();
        param.put("userEmail", userEmail);

        Map<String,Object> resultMap = loginMapper.selectUsrInfo(param);
        if (resultMap == null) {
            errorResult.put("message","아이디 또는 비밀번호를 잘못 입력하였습니다. <br/> 입력하신 내용을 다시 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        String userUid = resultMap.get("userUid").toString();
        param.put("userUid", userUid);

        // 계정 삭제 여부 체크
        String blYn = resultMap.get("blYn").toString();
        String blReason = String.valueOf(resultMap.get("blReason"));
        String blManager = String.valueOf(resultMap.get("blManager"));
        String blManagerMail = String.valueOf(resultMap.get("blManagerMail"));
        String blMsg = blReason + "인 사유로 귀사 계정이 삭제 처리 되었습니다.<br/>"
                     + "구체적인 삭제 사유 확인 혹은 계정 복원 필요시, 관리자에게 연락바랍니다.<br/>"
                     + "▷관리자 : " + blManager + "<br/>"
                     + "▷Mail  : " + blManagerMail
                ;
                //"하기 사유로 귀사 계정이 삭제 처리 되었습니다.<br/>" + "삭제 사유 확인 혹은 계정 복원 필요시, 관리자에게 연락바랍니다."
        if (Objects.equals("Y", blYn)) {
            errorResult.put("message",blMsg);
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        // 비밀번호 실패 횟수 추출
        int failCnt = fnGetFailCount(resultMap);
        String unLocking = resultMap.get("unLocking").toString();
        String waitingTime = resultMap.get("waitingTime").toString();

        // 비밀번호 5회 이상 불일치 && 30분 이내 (로그인 불가)
        // 비밀번호 5회 이상 계정장금에 대한 초기화 : 30분경과시 계정잠금해제 (로그인 가능)
        if (failCnt >= 5 && "N".equals(unLocking)) {
            String msg = "로그인을 5회 연속 실패로 계정잠금 처리되어 이용이 " + waitingTime + "분동안 제한됩니다.<br/>"
                       + "즉시 사용을 원하시면 임시 비밀번호 발급 바랍니다. (하기 버튼 참조)"
                       ;
            errorResult.put("message",msg);
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        // 비밀번호 인증
        Authentication resultAuth = null;
        try {
            resultAuth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userEmail, userPwd));
        } catch (BadCredentialsException e) {
            String msg = "";
            failCnt++;

            // 비밀번호 5회 이상 오류
            if(failCnt == 5) {
                msg = "로그인을 5회 연속 실패로 계정잠금 처리되어 이용이 30분 동안 제한됩니다.<br/>"
                        + "즉시 사용을 원하시면 임시 비밀번호 발급 바랍니다. (하기버튼 참조)"
                        ;
            } else if (failCnt > 5) {
                // 30분경과시 계정잠금해제 : 비밀번호 오류회수 초기화
                if ("Y".equals(unLocking)) {
                    failCnt = 1;
                    //msg = "비밀번호가 일치하지 않습니다. (현재 " + failCnt + "ghl");
                    msg = "아이디 또는 비밀번호를 잘못 입력하였습니다. <br/> 입력하신 내용을 다시 확인해주세요.";
                } else {
                    msg = "로그인을 5회 연속 실패로 계정 잠금 처리되어 이용이 " + waitingTime + "분동안 제한됩니다.<br/>"
                        + "즉시 사용울 원하시면 임시 비밀번호 발급 바랍니다. (하기 버튼 참조)"
                        ;
                }
            } else {
                //msg = "비밀번호가 일치하지 않습니다. (현재 " + failCnt + "회";
                msg = "아이디 또는 비밀번호를 잘못 입력하였습니다. <br/> 입력하신 내용을 다시 확인해주세요.";
            }

            param.put("failCnt", failCnt);
            loginMapper.updateUsrLoginFail(param);
            errorResult.put("message",msg);
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        // 비밀번호 초기화 여부 체크
        if (Objects.equals(resultMap.get("userPwdInitYn").toString(), "Y")) {
            Map<String, Object> rtnResult = new HashMap<>();
            rtnResult.put("pwdInitYn", "Y");
            rtnResult.put("userUid", resultMap.get("userUid").toString());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001", rtnResult);
        }

        // 비밀번호 변경일 초과 여부
        long daysBetweenPwdChanged = fnGetDaysBetween(resultMap.get("userPwdChD").toString());
        if (daysBetweenPwdChanged > 90 ) {
            errorResult.put("message", "비밀번호 변경일이 90일 초과되어 계정이 잠겼습니다.<br/>" + "임시 비밀번호 발급바랍니다.(하기 버튼 참조)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        // 최근 로그인일 초과 여부
        long daysBetweenLogin = fnGetDaysBetween(resultMap.get("userLoginD").toString());
        if (daysBetweenLogin > 90) {
            errorResult.put("message", "최근 접속일이 90일 초과되어 계정이 잠겼습니다.<br/>" + "임시 비밀번호 발급바랍니다.(하기 버튼 참조)");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        // 로그인 성공시 실패 회수 초기화
        loginMapper.updateUsrLoginSuccess(param);

        // access Log
        param.put("accessIp", fnGetClientIp());
        param.put("companyNo", resultMap.get("companyNo").toString());
        param.put("companyNm", resultMap.get("companyNm").toString());
        param.put("userNm", resultMap.get("userNm").toString());
        loginMapper.insertAccessLog(param);

        // 권한 부여 및 토큰 생성
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(resultAuth);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, req, res);

        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenUtils.TOKEN_USER_TYPE, resultMap.get("userRole"));
        claims.put(TokenUtils.TOKEN_USER_NM, resultMap.get("userNm"));
        claims.put(TokenUtils.TOKEN_USER_ID, resultMap.get("userEmail"));
        claims.put(TokenUtils.TOKEN_USER_EMP_NO, resultMap.get("userUid"));
        claims.put(TokenUtils.TOKEN_DEPT_ID, "");
        claims.put(TokenUtils.TOKEN_DEPT_NM, resultMap.get("companyNm"));
        claims.put(TokenUtils.TOKEN_COMPANY_ORIGIN_CD, resultMap.get("accessIp"));

        String token = tokenUtils.generateToken(claims);

        // 중복 로그인 차단 사용시
        if("true".equals(checkDupIP)) {
            //중복 로그인 검증 로직
            Map<String,Object> usrTokenMap = new HashMap<>();
            usrTokenMap.put("tokenUsrId", resultMap.get("userUid").toString());
            Map<String,Object> aliveToken = tokenMapper.selectTokenAccess(usrTokenMap);

            //다른 Pc에서 다른 IP or 접속시 기존 토큰 접근 차단
            if(aliveToken != null && !(Objects.equals(fnGetClientIp(), aliveToken.get("userIpAddress")))) {
                String sb = "webapi token create process " +
                        "ip :" +
                        fnGetClientIp() +
                        " /" +
                        "client";

                aliveToken.put("tokenDeniedDesc", sb);
                tokenMapper.insertTokenBanned(aliveToken);
            }

            //새로 생성된 토큰 DB저장
            Map<String,Object> newTokenMap = new HashMap<>();
            newTokenMap.put("tokenUserId",   resultMap.get("userUid").toString());
            newTokenMap.put("userIpAddress", fnGetClientIp());
            newTokenMap.put("userAgent",     fnGetBrowserInfo(req));
            newTokenMap.put("tokenKey",     token);
            tokenMapper.insertTokenAccess(newTokenMap);
        }

        new CookieUtil().createCookie(TokenUtils.TOKEN_KEY_HEADER, token, res, serverType);

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001", "정상적으로 처리되었습니다.");
    }

    /**
     * 비밀번호 잠금 해제  - 인증번호 이메일 발송
     *
     * @throws Exception
     */
    @Override
    @Transactional
    public ResponseEntity<?> unlockPwdProcess(Map<String, Object> param) throws Exception {
        Map<String,Object> errorResult = new HashMap<>();

        param.put("userEmail", String.valueOf(param.get("id")));
        //param.put("userUid", param.get("id").toString());
        Map<String, Object> resultMap = loginMapper.selectUsrInfo(param);
        if (resultMap == null) {
            //errorResult.put("message", "아이디 또는 비밀번호를 잘못 입력하였씁니다. <br> 입력하신 내용을 다시 확인해주세요.");
            errorResult.put("message", "해당 아이디로 임시비밀번호를 발송하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        // 비밀번호 실패 횟수 추출
        int failCnt = fnGetFailCount(resultMap);

        // 비밀번호 변경일 및 최근 로그인일 계산
        long daysBetweenPwdChanged = fnGetDaysBetween(resultMap.get("userPwdChD").toString());
        long daysBetweenLogin = fnGetDaysBetween(resultMap.get("userLoginD").toString());

        boolean failCntLock = failCnt < 5; // 비밀번호 실패 횟수가 5회 미만
        boolean pwdChangedLock = daysBetweenPwdChanged <= 90; // 비밀번호 변경일이 90일 이하
        boolean loginLock = daysBetweenLogin <= 90; // 최근 로그인일이 90일 이하

        if (failCntLock && pwdChangedLock && loginLock) {
            errorResult.put("message", "계정 잠금 이력이 없습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        String tempPassword = fnSetTempPassword();
        String crytPwd = BCrypt.hashpw(tempPassword, BCrypt.gensalt());
        param.put("userEmail", resultMap.get("userEmail").toString());
        //param.put("userEmail", resultMap.get("userUid").toString()); //?? usrId =>userUid 변경하라는 것?
        param.put("userUid", resultMap.get("userUid").toString());
        param.put("crytPwd", crytPwd);
        param.put("password", tempPassword);

        // 임시비밀번호 저장
        try {
            loginMapper.updateUsrTempPwd(param);
        }catch (Exception e){
            log.error("unlockPwdProcess : {}", e.getMessage());
            errorResult.put("message", "서버내 오류가 발생 하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        // 이메일 발송
        try {
            fnSendMail(param);
        }catch(Exception e){
            errorResult.put("message","[임시 비밀번호 발급 안내 : RTPE107] <br> 이메일 전송 중 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        Map<String, Object> rtnResult = new HashMap<>();
        rtnResult.put("tempPassword", "임시비밀번호 : " + tempPassword);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001", rtnResult);
    }

    /**
     * 비밀번호 변경
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public ResponseEntity<?> changePwdProcess(LoginModel loginModel) throws Exception {
        Map<String,Object> errorResult = new HashMap<>();
        Map<String,Object> param = new ObjectMapper().convertValue(loginModel, Map.class);

        String userUid = Objects.toString(param.get("userUid"), "");
        if (userUid.isBlank()) {
            errorResult.put("message", "잘못된 접근입니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        String oldPassword = Objects.toString(param.get("password"), "");
        String newPassword = Objects.toString(param.get("newPassword"), "");
        if (newPassword.isBlank()) {
            errorResult.put("message", "신규 비밀번호를 입력하세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }else if (Objects.equals("Y", param.get("myInfoYn")) && oldPassword.isBlank()) { // 마이페이지
            errorResult.put("message", "기존 비밀번호를 입력하세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        Map<String, Object> usrMap = loginMapper.findUsrInfoByUid(param);
        if (!oldPassword.isBlank() && !BCrypt.checkpw(oldPassword, usrMap.get("userPwd").toString())) { // 마이페이지
            errorResult.put("message", "기존 비밀번호가 일치하지 않습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        } else if (BCrypt.checkpw(newPassword, usrMap.get("userPwd").toString())) {
            errorResult.put("message", "기존 비밀번호와 동일합니다.<br/>"+"다른 비밀번호를 입력해주세요");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        String encodePassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        param.put("crytPwd", encodePassword);
        try {
            loginMapper.updateUsrPwd(param);
        }catch (Exception e){
            log.error("changePwdProcess : {}", e.getMessage());
            errorResult.put("message", "서버 내 오류가 발생 하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        Map<String, Object> rtnResult = new HashMap<>();
        rtnResult.put("message", "비밀번호 변경이 완료되었습니다.<br/>"+"변경하신 비밀번호로 다시 로그인 해주세요.");
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001", rtnResult);
    }

    /**
     * id 찾기
     */
    @Override
    public ResponseEntity<?> idFindProcess(Map<String, Object> param) {
        HashMap<String,Object> errorResult  = new HashMap<>();
        errorResult.put("staus", "error");

        Map<String,Object> rtnResult = loginMapper.findUsrEmail(param);
        if (rtnResult == null) {
            errorResult.put("message", "해당 정보로 조회된 이메일이 없습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001", rtnResult);
    }

    /**
     * 비밀번호 찾기 - 임시 메일 발송
     */
    @Override
    @Transactional
    public ResponseEntity<?> pwdFindProcess(Map<String, Object> param) throws Exception {
        Map<String,Object> errorResult = new HashMap<>();
        errorResult.put("staus", "error");
        param.put("userEmail", param.get("id").toString());

        Map<String, Object> resultMap = loginMapper.selectUsrInfo(param) ;
        if (resultMap == null) {
            //errorResult.put("message", "아이디 또는 비밀번호를 잘못 입력하였습니다. <br> 입력하신 내용을 다시 확인해주세요.");
            errorResult.put("message", "해당 아이디로 임시비밀번호를 발송하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        String tempPassword = fnSetTempPassword();
        String crytPwd = BCrypt.hashpw(tempPassword, BCrypt.gensalt());
        param.put("userUid", resultMap.get("userUid").toString());
        param.put("userEmail",resultMap.get("userEmail").toString());
        param.put("crytPwd", crytPwd);
        param.put("password", tempPassword);

        // 임시 비밀번호 저장
        try {
            loginMapper.updateUsrTempPwd(param);
        }catch (Exception e) {
            log.error("pwdFindProcess : {}", e.getMessage());
            errorResult.put("message", "임시 비밀번호 저장 중 서버 내 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        // 이메일 발송
        try {
            fnSendMail(param);
        }catch(Exception e){
            errorResult.put("message", "[비밀번호찾기_임시 비밀번호 발급 안내 : RTPE107] <br> 이메일 전송 중 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001", errorResult);
        }

        Map<String, Object> rtnResult = new HashMap<>();
        rtnResult.put("tempPassword", "임시비밀번호 :" + tempPassword);
        rtnResult.put("message", "등록된 이메일로 임시 비밀번호가 발급되었습니다.");
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001", rtnResult);
    }

    /**
     * 이메일 전송
     */

    void fnSendMail(Map<String, Object> param) {
        try {
           SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
           //simpleMailMessage.setTo(param.get("userEmail").toString());
           simpleMailMessage.setTo("blbl9857@naver.com");
           simpleMailMessage.setSubject("[HSYNC] 기술협력제안 임시비밀번호 안내");
           simpleMailMessage.setText("임시비밀번호 : " + param.get("password").toString());
           javaMailSender.send(simpleMailMessage);
        }catch(Exception e) {
            log.error("fnSendMail : {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 비밀번호 실패 횟수 호출
     */
    private int fnGetFailCount(Map<String, Object> usrMap) {
        if (usrMap != null && usrMap.get("failCnt") != null) {
            return Integer.parseInt(usrMap.get("failCnt").toString());
        }
        return 0;
    }

    /**
     * 날짜계산 : 입력일자 ~ 오늘날짜 까지 일수
     */
    private  long fnGetDaysBetween(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return ChronoUnit.DAYS.between(date, LocalDate.now());
    }

    /**
     * 임시 비밀번호
     */
    private String fnSetTempPassword() {
        int length = 10;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" // uppercase
                + "abcdefghijklmnopqrstuvwxyz" // lowercase
                + "0123456789" // numbers
                + "@&!%*?&"; // special symbols
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    /**
     * ip 정보
     */
    private String fnGetClientIp() throws UnknownHostException {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getHeader("X-Forwarded-For");

            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("Wl-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("X-Real-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("X-RealIP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getHeader("REMOTE_ADDR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getRemoteAddr();
            }

            if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1")) {
                InetAddress address = InetAddress.getLocalHost();
                ip = address.getHostAddress();
            }

            //여러 프록시를 거치는 경우 첫번째 IP 사용
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
        return null;
    }

    private String fnGetBrowserInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }

    /**
     * 인증 JWT 토큰 갱신
     */
    @Override
    public ResponseEntity<?> refreshToken() throws Exception {

        // 사용자 ID
        String userEmail = Objects.toString(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID), "");
        if(!userEmail.isBlank() && !(boolean)tokenUtils.getClaimsByValue(TokenUtils.TOKEN_EXPIRATION_VALID)) {
            //로그인 계정 정보 찾기
            Map<String, Object> param = loginMapper.selectUserByEmail(userEmail);
            param.put("userType", param.get("userRole"));
            
            // 시큐리티 재인증
            CoreWebApiUserDetail coreWebApiUserDetail = new CoreWebApiUserDetail(param);
            AbstractAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    coreWebApiUserDetail, coreWebApiUserDetail.getPassword(), coreWebApiUserDetail.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            securityContextRepository.saveContext(securityContext, req, res);
            String refreshToken = tokenUtils.refreshToken(TokenUtils.TOKEN_REFRESH_DEFAULT);

            // 중복 로그인 차단 사용시
            if ("true".equals(checkDupIP)) {

                Map<String, Object> tmpMap = new HashMap<>();
                tmpMap.put("tokenUserId", userEmail);
                Map<String, Object> beforeTokenMap = tokenMapper.selectTokenAccess(tmpMap);

                String beforeToken = beforeTokenMap.get("tokenKey").toString();
                String sb = "web token refresh process";
                Map<String, Object> aliveToken = new HashMap<>();
                aliveToken.put("tokenUserId", userEmail);
                aliveToken.put("tokenKey", beforeToken);
                aliveToken.put("tokenDeniedDesc", sb);
                tokenMapper.insertTokenBanned(aliveToken);

                Map<String, Object> newToken = new HashMap<>();
                newToken.put("tokenKey", refreshToken);
                newToken.put("tokenUserId", userEmail);
                newToken.put("usrIpAddress", fnGetClientIp());
                newToken.put("usrAgent", fnGetBrowserInfo(req));
                tokenMapper.insertTokenAccess(newToken);
            }

            // 토큰 생성
            new CookieUtil().createCookie(TokenUtils.TOKEN_KEY_HEADER, refreshToken, res, serverType); // // 단일 도메인일 경우 local로 해도 됨.

            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "토큰이 갱신 되었습니다.");
        }

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", "Token is invalid");
    }
}