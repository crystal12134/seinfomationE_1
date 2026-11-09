package com.seinfo.seinfomationA.component.member.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seinfo.seinfomationA.component.file.service.impl.TusServiceImpl;
import com.seinfo.seinfomationA.component.member.mapper.DropInfoMapper;
import com.seinfo.seinfomationA.component.member.mapper.MemberMapper;
import com.seinfo.seinfomationA.component.member.model.CompanyModel;
import com.seinfo.seinfomationA.component.member.model.MemberModel;
import com.seinfo.seinfomationA.component.member.service.MemberService;
import com.seinfo.seinfomationA.util.EncriptUtil;
import com.seinfo.seinfomationA.util.api.ApiService;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    /** 아이디 중복 확인은 안내용이며 최종 저장은 DB UNIQUE 제약으로도 보호합니다. */
    @Override
    public ResponseEntity<?> checkLoginIdProcess(String value) {
        String loginId = com.seinfo.seinfomationA.util.LoginIdPolicy.normalize(value);
        Map<String, Object> result = new HashMap<>();
        if (!com.seinfo.seinfomationA.util.LoginIdPolicy.isValid(loginId)) {
            result.put("message", "아이디는 영문자로 시작하는 영문 소문자·숫자·밑줄(_) 4~20자로 입력해주세요.");
            return responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", result);
        }
        boolean available = memberMapper.chkLoginIdDuplicate(Map.of("loginId", loginId)) == 0;
        result.put("available", available);
        result.put("loginId", loginId);
        result.put("message", available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.");
        return responseEntityUtil.getResponseEntity(available ? CodeCoreResultList.RESULT_CODE_SUCCESS : CodeCoreResultList.RESULT_CODE_FAILED, available ? "S001" : "E001", result);
    }


    private final MemberMapper memberMapper;
    private String verificationKey(String email) {
        var attributes=(org.springframework.web.context.request.ServletRequestAttributes)org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest().getSession(true).getId()+":"+email.trim().toLowerCase(java.util.Locale.ROOT);
    }
    private final TokenUtils tokenUtils;
    private final DropInfoMapper dropInfoMapper;
    private final ResponseEntityUtil responseEntityUtil;
    private final TusServiceImpl tusService;
    private final ApiService apiService;

    @Value("${seinfo.openApi.company.url}")
    private String openApiUrl;
    @Value("${seinfo.openApi.company.serviceKey}")
    private String serviceKey;
    // 인증번호 저장할 캐쉬 로드
    private final Cache<String, Map<String, Object>> verificationCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(10000).build();

    /**
     * 회원가입 정보 저장
     *
     * @throws Exception
     */

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public void setMemberInfoAdd(MemberModel memberModel, CompanyModel companyModel) {

        if (companyModel == null) {
            throw new IllegalArgumentException("사업자 인증부터 다시 진행해주세요.");
        }
        String loginId = com.seinfo.seinfomationA.util.LoginIdPolicy.requireValid(memberModel.getLoginId());
        memberModel.setLoginId(loginId);
        memberModel.setUserEmail(memberModel.getUserEmail().trim());
        if (memberMapper.chkLoginIdDuplicate(Map.of("loginId", loginId)) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (memberMapper.chkUserEmailDuplicate(Map.of("userEmail", memberModel.getUserEmail())) > 0) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        Map<String, Object> verified = verificationCache.getIfPresent(verificationKey(memberModel.getUserEmail()));
        if (verified == null || !Boolean.TRUE.equals(verified.get("verified"))
                || System.currentTimeMillis() - (long) verified.get("timestamp") > TimeUnit.MINUTES.toMillis(5)) {
            throw new IllegalArgumentException("이메일 인증을 완료한 뒤 다시 가입해주세요.");
        }

        Map<String, Object> memberMap = new ObjectMapper().convertValue(memberModel, Map.class);
        Map<String, Object> companyMap = new ObjectMapper().convertValue(companyModel, Map.class);

        // 패스워드 암호화
        String password = memberModel.getUserPwd();
        String encodedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // 사업자번호 암호화
        try {
            String encodedCompanyNo = EncriptUtil.lookupCompanyNo(companyModel.getCompanyNo());
            memberMap.put("companyNo", encodedCompanyNo);
            memberMap.replace("userPwd", encodedPassword);
            memberMap.put("regUserId", memberModel.getUserEmail());
            memberMap.put("modUserId", memberModel.getUserEmail());

            // 회사 사업자 번호
            memberMap.put("userRole", "USER"); // 직원 권한은 관리자 승인으로만 부여

            companyMap.replace("companyNo", encodedCompanyNo);
            companyMap.put("companyNoEnc", EncriptUtil.encryptCompanyNo(companyModel.getCompanyNo()));
            companyMap.put("regUserId", memberModel.getUserEmail());
            companyMap.put("modUserId", memberModel.getUserEmail());

        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("setMemberInfoAdd >>>> 암호화 오류 발생: {}", e.toString());
            throw new RuntimeException("사업자번호 암호화 중에 오류가 발생하였습니다.");
        }

        if (!memberMapper.insertVendor(companyMap)) {
            throw new RuntimeException("회사 정보 데이터 삽입 중 오류가 발생하였습니다");
        }

        if (!memberMapper.insertVendorManager(memberMap)) {
            throw new RuntimeException("회원 정보 데이터 삽입 중 오류가 발생하였습니다");
        }
        verificationCache.invalidate(verificationKey(memberModel.getUserEmail()));
      }

    /**
     * 사업자번호 검증
     *
     * @throws Exception
     */

    @SuppressWarnings("unchecked")
    @Override
    public ResponseEntity<?> checkBizNumberProcess(String companyNo, String ceoNm, String companyD, String companyNm) {
        HashMap<String, Object> errorResult = new HashMap<>();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> bizInfo = new HashMap<String, Object>();

        bizInfo.put("b_no", companyNo);
        bizInfo.put("p_nm", ceoNm);
        bizInfo.put("start_dt", companyD);
        bizInfo.put("b_nm", companyNm);

        List<Object> businesses = new ArrayList<>();
        businesses.add(bizInfo);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("businesses", businesses);

        try {
            String externalApiUrl = openApiUrl + serviceKey;
            URI uri = new URI(externalApiUrl);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) { // 외부 API 요청의 응답 상태 코드가 200번대 확인
                errorResult.put("message", "API 요청 실패. 상태 코드: " + response.getStatusCode());
                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            List<Map<String, Object>> dataList = objectMapper
                    .convertValue(responseMap.getOrDefault("data", new ArrayList<>()), new TypeReference<>() {});

            if (dataList.isEmpty() || !Objects.equals(dataList.get(0).get("valid"), "01")) {
                errorResult.put("message", "유효하지 않은 사업자등록번호입니다.");
                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
            }

            Map<String, Object> status = (Map<String, Object>) dataList.get(0).get("status");
            String bSttCd = status.get("b_stt_cd").toString();

            if ("01".equals(bSttCd)) {
                HashMap<String, Object> rtnResult = new HashMap<>();
                rtnResult.put("message", "사업자등록번호가 유효합니다.");
                var current=(org.springframework.web.context.request.ServletRequestAttributes)org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
                current.getRequest().getSession().setAttribute("VERIFIED_BUSINESS",com.seinfo.seinfomationA.util.security.BusinessVerification.key(companyNo,ceoNm,companyD,companyNm));
                current.getRequest().getSession().setAttribute("VERIFIED_BUSINESS_AT",System.currentTimeMillis());
                rtnResult.put("ceoNm", ceoNm);
                rtnResult.put("companyNo", companyNo);
                rtnResult.put("companyD", companyD);
                rtnResult.put("companyNm", companyNm);

                String encodedCompanyNo = EncriptUtil.lookupCompanyNo(companyNo);
                Map<String, Object> companyMap = memberMapper.chkCompanyNoDuplicate(encodedCompanyNo);
                if(companyMap != null) {
                    rtnResult.put("duplicateYn", true);
                    rtnResult.put("companyAddress", companyMap.get("companyAddress"));
                    rtnResult.put("companyAddressDetail", companyMap.get("companyAddressDetail"));
                    rtnResult.put("ceoPhone", companyMap.get("ceoPhone"));
                    rtnResult.put("ceoEmail", companyMap.get("ceoEmail"));
                    rtnResult.put("companyUrl", companyMap.get("companyUrl"));
                    rtnResult.put("companyInfo", companyMap.get("companyInfo"));
                }

                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnResult);
            }

            errorResult.put("message", "유효하지 않은 사업자등록번호입니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } catch (JsonProcessingException e) {
            errorResult.put("message", "JSON 파싱 오류 발생");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("Exception : {}", e.getMessage());
            //errorResult.put("message", "API 호출 중 오류 발생");
            errorResult.put("message", "API 호출 중 오류 발생");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
    }

    /**
     * 인증번호 이메일 발송
     *
     * @throws Exception
     */

    @Override
    public ResponseEntity<?> emailCheckProcess(String userEmail) {
        userEmail = userEmail.trim();
        Map<String, Object> param = new HashMap<String, Object>();
        HashMap<String, Object> errorResult = new HashMap<>();

        // 이메일 중복여부 체크
        param.put("userEmail", userEmail);
        int duplicateTest = memberMapper.chkUserEmailDuplicate(param);
        if (duplicateTest >= 1) {
            errorResult.put("message", "사용자 계정이 중복됩니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        // 인증번호 생성
        int length = 6;
        String chars = "0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder authNumber = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            authNumber.append(chars.charAt(index));
        }

        // 인증번호 저장
        verificationCache.put(verificationKey(userEmail), Map.of("authNumber", authNumber.toString(), "timestamp", System.currentTimeMillis(), "createdAt", System.currentTimeMillis(), "attempts", 0));

        // 인증번호 메일발송
        try {
            //param.put("userEmail", userEmail+";");
            param.put("userEmail", userEmail.trim()); //sdc
            param.put("mailTitle", "[SE정보기술] 사용자 인증번호 안내");
            param.put("mailContent", authNumber);

            apiService.sendMail(param, "seinfo/mail/mailTemplate04");
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("emailCheckProcess >>> fnSendMail : {}", e.getMessage());
            errorResult.put("message", "[사용자 인증 안내 : RTPE108] <br> 이메일 전송 중 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        HashMap<String, Object> rtnResult = new HashMap<>();
        // 인증번호는 이메일로만 전송합니다. 브라우저 응답에는 포함하지 않습니다.
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnResult); // 왜 인증번호를 프론트에 보내냐? 프론트에도 인증번호를 내려줍니다. (개발 또는 테스트 환경) 위해서
    }

    /**
     * 인증번호 유효 시간 연장
     *
     * @throws Exception
     */
    @Override
    public ResponseEntity<?> extendTime(String userEmail) { // 인증시간 연장
        HashMap<String, Object> rtnResult = new HashMap<>();
        // (key, existingData) -> ...는 람다식
        verificationCache.asMap().computeIfPresent(verificationKey(userEmail), (key, existingData) -> {
            long created=((Number)existingData.getOrDefault("createdAt",existingData.get("timestamp"))).longValue();
            if(System.currentTimeMillis()-created>TimeUnit.MINUTES.toMillis(3) || Boolean.TRUE.equals(existingData.get("extended"))) return existingData;
            Map<String,Object> updated=new HashMap<>(existingData);
            updated.put("timestamp",System.currentTimeMillis());updated.put("extended",true);return updated;
        });

        rtnResult.put("message", "시간이 연장되었습니다.");
        rtnResult.put("userEmail", userEmail);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnResult);
    }

    /**
     * 인증번호 검증
     *
     * @throws Exception
     */
    @Override
    public ResponseEntity<?> checkAuthNumberProcess(String userEmail, String authNumber) { // 인증번호 체크

        HashMap<String, Object> errorResult = new HashMap<>();

        Map<String, Object> data = verificationCache.getIfPresent(verificationKey(userEmail));

        if (data == null) {
            errorResult.put("message", "인증번호를 확인할 수 없습니다. 다시 요청해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        long storedTime = (long) data.get("timestamp");
        long elapsedTime = System.currentTimeMillis() - storedTime;
        long maxTime = TimeUnit.MINUTES.toMillis(3); // 3분

        if (elapsedTime > maxTime) {
            verificationCache.invalidate(verificationKey(userEmail)); // 만료된 인증번호 삭제
            errorResult.put("message", "인증번호 확인 시간이 초과하였습니다. 다시 요청해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        int attempts=((Number)data.getOrDefault("attempts",0)).intValue()+1;
        if(attempts>5) {
            verificationCache.invalidate(verificationKey(userEmail));
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.");
        }
        Map<String,Object> attemptData=new HashMap<>(data);attemptData.put("attempts",attempts);
        verificationCache.put(verificationKey(userEmail),attemptData);
        if (!authNumber.equals(data.get("authNumber"))) {
            errorResult.put("message", "인증번호가 일치하지 않습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        Map<String, Object> verifiedData = new HashMap<>(data);
        verifiedData.put("verified", true);
        verifiedData.put("timestamp", System.currentTimeMillis());
        verificationCache.put(verificationKey(userEmail), verifiedData);

        HashMap<String, Object> rtnResult = new HashMap<>();
        rtnResult.put("message", "인증성공");
        rtnResult.put("userEmail", userEmail);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnResult);
    }

    /**
     * 회원정보 불러오기
     */

    @Override
    public ResponseEntity<?> getMemberInfo() {
        Map<String, Object> errorResult = new HashMap<String, Object>();

        //토큰 유효성 체크
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            errorResult.put("message", "로그인이 필요합니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
        if (tokenUtils.getTokenFromCookie() == null) {
            errorResult.put("message", "잘못된 접근입니다. 로그인이 필요합니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        String companyNo = "";
        String decodedCompanyNo = "";
        String userEmail = authentication.getName();

        Map<String, Object> rtnResult = memberMapper.selectUsrAndCompanyInfo(Map.of("userEmail", userEmail));

        if (rtnResult == null) {
            log.error("getMemberInfo >>>> 등록되지 않은 사용자: {}", userEmail);
            errorResult.put("message", "등록되지 않은 사용자입니다. 새로 로그인이 필요합니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } else {
            try {
                if (rtnResult.get("companyNo") != null) {
                    companyNo = rtnResult.get("companyNo").toString();
                    decodedCompanyNo = EncriptUtil.decryptCompanyNo(rtnResult.get("companyNoEnc").toString());
                    if(!EncriptUtil.lookupCompanyNo(decodedCompanyNo).equals(companyNo)) throw new SecurityException("회사 암호문 연결이 일치하지 않습니다.");
                    StringBuffer sb = new StringBuffer(decodedCompanyNo);
                    sb.insert(3, '-');
                    sb.insert(6, '-');
                    rtnResult.replace("companyNo", sb.toString());
                    rtnResult.remove("companyNoEnc");
                }
            } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
                log.error("getMemberInfo >>>> 복호화 오류 발생 : {}", e.toString());
                errorResult.put("message", "서버 내 오류가 발생하였습니다.");
                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
            }
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnResult);
    }

    /**
     * 회원정보 업데이트
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setMyInfoUpd(MemberModel memberModel, CompanyModel companyModel) {
        String uid=Objects.toString(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_EMP_NO), "");
        if(!uid.equals(String.valueOf(memberModel.getUserUid()))) throw new org.springframework.security.access.AccessDeniedException("본인 정보만 수정할 수 있습니다.");
        memberModel.setUserEmail(String.valueOf(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID)));


        Map<String, Object> memberMap = new ObjectMapper().convertValue(memberModel, Map.class);
        Map<String, Object> companyMap = new ObjectMapper().convertValue(companyModel, Map.class);
        Map<String, Object> resultMap = memberMapper.findUserByUserUid(memberMap);
        if (!BCrypt.checkpw(memberMap.get("userPwd").toString(), resultMap.get("userPwd").toString())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }
        //사업자 번호 암호화
        try {
            String encodedCompanyNo = EncriptUtil.lookupCompanyNo(companyModel.getCompanyNo());
            companyMap.put("companyNo", resultMap.get("companyNo"));

            memberMap.put("modUserId", memberModel.getUserEmail());
            companyMap.put("modUserId", memberModel.getUserEmail());
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("setMyInfoUpd >>>> 암호화 오류 발생: {}", e.getMessage());
            throw new RuntimeException("서버내 오류가 발생하였습니다");
        }
        if (!memberMapper.updateUserInfo(memberMap)) {
            throw new RuntimeException("회원 정보 수정 중 오류가 발생하였습니다. (사용자정보)");
        }
        if (!memberMapper.updateVendorInfo(companyMap)) {
            throw new RuntimeException("회원 정보 수정 중 오류가 발생하였습니다. (회사정보)");
        }
    }

    /**
     * 회원탈퇴
     */

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public ResponseEntity<?> dropUserInfo(Map<String, Object> param) throws Exception {
        Map<String, Object> errorResult = new HashMap<String, Object>();
        param.put("userId", this.tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_EMP_NO));

        Map<String, Object> userMap = dropInfoMapper.selectDropUsrInfo(param);

        if(!BCrypt.checkpw(param.get("userPwd").toString(), userMap.get("userPwd").toString())) {
            errorResult.put("message", "비밀번호가 일치하지 않습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        // 물리적 데이터 삭제
        List<Map<String, Object>> memberFileList = dropInfoMapper.selectMfileList(param);
        if(!memberFileList.isEmpty()) {
            ResponseEntity<?> rtn = tusService.deleteFile(memberFileList);
            // 응답에서 process_code 및 data 추출
            Map<String, Object> resultMap = (Map<String, Object>) rtn.getBody();
            String processCode = (String) Objects.requireNonNull(resultMap).get("process_code");

            if (!"00".equals(processCode)) {
                Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                String errorMsg = (String) data.get("message");

                String msg = "삭제 중 서버 내 오류가 발생하였습니다. (파일삭제) <br> ▷ " + errorMsg;
                log.error("dropUserInfo >>> deleteFile >>> message : {}", msg);
                throw new RuntimeException(msg);
            }
        }

        // 회원 데이터 삭제
        List<String> memberList = new ArrayList<>();
        memberList.add(this.tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_EMP_NO).toString());
        dropInfoMapper.deleteUsrAllTables(memberList);
        
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "정상적으로 처리되었습니다.");
    }
}