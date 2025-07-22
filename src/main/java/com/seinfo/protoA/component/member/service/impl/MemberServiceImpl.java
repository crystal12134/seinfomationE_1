package com.seinfo.protoA.component.member.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seinfo.protoA.component.file.service.impl.TusServiceImpl;
import com.seinfo.protoA.component.member.mapper.DropInfoMapper;
import com.seinfo.protoA.component.member.mapper.MemberMapper;
import com.seinfo.protoA.component.member.model.CompanyModel;
import com.seinfo.protoA.component.member.model.MemberModel;
import com.seinfo.protoA.component.member.service.MemberService;
import com.seinfo.protoA.util.EncriptUtil;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
import com.seinfo.protoA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private final MemberMapper memberMapper;
    private final TokenUtils tokenUtils;
    private final DropInfoMapper dropInfoMapper;
    private final ResponseEntityUtil responseEntityUtil;
    private final JavaMailSender javaMailSender;
    private final TusServiceImpl tusService;

    @Value("${tus.server.data.directory:/tmp}")
    private String savePath;
    @Value("${seinfo.sendMail.token:null}")
    private String token;
    @Value("${seinfo.openApi.company.url}")
    private String openApiUrl;
    @Value("${seinfo.openApi.company.serviceKey}")
    private String serviceKey;
    // 인증번호 저장할 캐쉬 로드
    private final Cache<String, Map<String, Object>> verificationCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(10000).build(); // 캐시에 저장할 자료의 형식을 지정함.userEmail(문자열)을 키로 하고, Map 형태의 데이터 (예: 인증번호, 시간 등)를 값으로 저장
                                                                                        // Caffeine 라이브러리로 새 캐시를 만듦,캐시에 저장된 항목은 쓰기(write) 된 후 5분이 지나면 자동 삭제,최대 10,000개까지만 저장 가능. 넘으면 오래된 것부터 삭제됨.

    /**
     * 회원가입 정보 저장
     *
     * @throws Exception
     */

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public void setMemberInfoAdd(MemberModel memberModel, CompanyModel companyModel) {

        Map<String, Object> memberMap = new ObjectMapper().convertValue(memberModel, Map.class); // Jackson 라이브러리의 ObjectMapper 는 자바 객체(memberModel)를 다른형태로 바꾸기 위해 사용, Map<String, Object> 형식으로 바꾸기 위해서 사용, 즉, 자바 클래스 → JSON 형태의 자료구조(Map 등)로
        Map<String, Object> companyMap = new ObjectMapper().convertValue(companyModel, Map.class); // 이렇게 Map방식으로 바꾸는 이유는 값 조작하기 쉽고 DB 바인딩이 쉬워서

        // 패스워드 암호화
        String password = memberModel.getUserPwd();
        String encodedPassword = BCrypt.hashpw(password, BCrypt.gensalt()); // BCrypt는 외부 보안 라이브러리 , hashpw()와 gensalt()는 BCrypt 클래스의 정적 메서드, 단방향 해시 복호화 x

        // 사업자번호 암호화
        try {
            String encodedCompanyNo = EncriptUtil.encryptAes256(companyModel.getCompanyNo()); // 사업자번호는 AES-256 대칭키 암호화(양방향) 사용→ 나중에 복호화 가능해야 하기 때문에 BCrypt가 아니라 AES 사용

            memberMap.put("companyNo", encodedCompanyNo);
            memberMap.replace("userPwd", encodedPassword);
            memberMap.put("regUserId", memberModel.getUserEmail());
            memberMap.put("modUserId", memberModel.getUserEmail());
            memberMap.put("userRole", "USER");

            companyMap.replace("companyNo", encodedCompanyNo); // put() 쓸 때 키가 없을 수도 있음 -> 새로 추가됨, replace()쓸때 키가 없으면 무시됨 (아무 일도 안 함)
            companyMap.put("regUserId", memberModel.getUserEmail());
            companyMap.put("modUserId", memberModel.getUserEmail());

        } catch (Exception e) {
            log.error("setMemberInfoAdd >>>> 암호화 오류 발생: {}", e.toString());
            throw new RuntimeException("사업자번호 암호화 중에 오류가 발생하였습니다.");
        }

        if (!memberMapper.insertVendor(companyMap)) {
            throw new RuntimeException("회사 정보 데이터 삽입 중 오류가 발생하였습니다");
        }

        if (!memberMapper.insertVendorManager(memberMap)) {
            throw new RuntimeException("회원 정보 데이터 삽입 중 오류가 발생하였습니다");
        }
      }

    /**
     * 사업자번호 검증
     *
     * @throws Exception
     */

    @SuppressWarnings("unchecked")
    @Override
    public ResponseEntity<?> checkBizNumberProcess(String companyNo, String ceoNm, String companyD, String companyNm) { // 사업자번호 유효성 검증"을 외부 API를 통해 수행하고, 그 결과를 사용자에게 전달하는 핵심 로직
        HashMap<String, Object> errorResult = new HashMap<>();

        RestTemplate restTemplate = new RestTemplate(); // HTTP 요청을 보내고 응답을 받아오는 도구 , 다른방법으로는 WebClient
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
        requestBody.put("businesses", businesses); // Map에 담고 → List에 넣고 → 다시 Map에 묶어서 POST로 보낼 형태로 만듬. * 외부 API가 요구하는 JSON 형식 때문

        try {
            String externalApiUrl = openApiUrl + serviceKey; // openApiUrl: 사업자등록 상태 확인용 정부 API 주소 , serviceKey: 인증키
            URI uri = new URI(externalApiUrl);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers); // requestBody(사업자정보),headers JSON 데이터타입 ,즉 외부 API로 요청을 보낼 때 "요청 본문(body)"과 "요청 헤더(headers)"를 묶어서 보낸다
            ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class); // RestTemplate을 사용해서 외부 공공데이터 API 호출,첫번쨰 인자 요청보낼주소,2번쨰인자 요청보낼 데이터타입,3번쨰인자 응답받을 타입

            if (!response.getStatusCode().is2xxSuccessful()) { // 외부 API 요청의 응답 상태 코드가 200번대 확인
                errorResult.put("message", "API 요청 실패. 상태 코드: " + response.getStatusCode());
                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {}); // getBody()는 HTTP 응답의 "본문(body(문자열))" 내용을 꺼내는 메서드, body는 JSON 형식의 문자열(String) 을 가지고 있다, readValue(문자열, 어떤 타입으로 변환할지);
                                                                                                                    // 자바에선 Map<String, Object> 같이 제네릭 타입은 일반적으로 한 줄로 지정하기 어렵다, new TypeReference<>()은 어떤 타입으로 바꿀지 알려줌 (Map<String, Object> 등
                                                                                                                    // Java 객체(Model) → Map 바꿀때, Map.class OK, 단순 key-value 변환이므로 타입 정보까지 필요 없음
                                                                                                                    // JSON(String) → Map으로 바꿀때 역직렬화, Map.class (x) TypeReference<>() 필요, 타입 정보(문자열,숫자,불리언 등)가 런타임에 사라지므로 정확한 타입 유지를 위해 TypeReference필요함
                                                                                                                    // convertValue()는 이미 자바 객체가 들어있어서, ObjectMapper가 내부 필드를 그대로 복사해도 문제가 안 생김, 자바 객체 → 다른 자바 타입 변환할때 사용
                                                                                                                    // readValue()는 JSON 문자열을 파싱해서 타입을 유추해야 하므로, 명확한 타입 정보 필요, JSON 문자열 → 자바 객체로 변환할떄 사용, 즉 TypeReference사용하면 알아서 타입 맞춰줌
            List<Map<String, Object>> dataList = objectMapper
                    .convertValue(responseMap.getOrDefault("data", new ArrayList<>()), new TypeReference<>() {}); // getOrDefault는 Map에 key가 존재하면 그 값을 반환, 없으면 두 번째 인자로 준 **기본값(default 값)**을 반환

            if (dataList.isEmpty() || !Objects.equals(dataList.get(0).get("valid"), "01")) { // 리스트에서 첫 번째 Map을 꺼냄-> 그 Map 안에서 "valid"라는 키 꺼냄
                                                                                                // Objects.equals은 a나 b 중 하나가 null이더라도 NPE가 발생하지 않게 안전하게 비교
                errorResult.put("message", "유효하지 않은 사업자등록번호입니다.");
                return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
            }

            Map<String, Object> status = (Map<String, Object>) dataList.get(0).get("status"); //  API에서 받은 첫 번째 사업자 정보 JSON 객체,그 안에 있는 "status"라는 필드의 값 (이것도 Map 형태)
                                                                                              // Map 안에서 key "status"에 해당하는 값을 꺼냄,그런데 Map.get()의 리턴 타입은 Object이다.
            String bSttCd = status.get("b_stt_cd").toString(); // status는 Map<String, Object> 타입, 그래서 status.get("b_stt_cd")의 리턴값은 Object 타입 그래서 문자열로 바꿔줌

            if ("01".equals(bSttCd)) {
                HashMap<String, Object> rtnResult = new HashMap<>();
                rtnResult.put("message", "사업자등록번호가 유효합니다.");
                rtnResult.put("ceoNm", ceoNm);
                rtnResult.put("companyNo", companyNo);
                rtnResult.put("companyD", companyD);
                rtnResult.put("companyNm", companyNm);

                String encodedCompanyNo = EncriptUtil.encryptAes256(companyNo);
                Map<String, Object> companyMap = memberMapper.chkCompanyNoDuplicate(encodedCompanyNo); // 입력한 사업자번호가 이미 DB에 등록되어 있는지 확인 , 리턴값이 Map<String, Object>인 이유는 DB 결과(1건)를 맵 형태로 받아오기 때문
                if(companyMap != null) { // 존재하면 그 회사 정보를 가져와서 응답 결과에 포함, 사용자가 다시 작성할 필요 없이 자동으로 채워주는 용도
                    rtnResult.put("duplicateYn", true); // 이미 등록된 사업자라면, 중복이라고 표시, duplicateYn"을 true로 리턴하면 프론트에서는 “이미 등록된 사업자입니다” 같은 메시지 띄어줄수 있음
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
        int length = 6; // 인증번호 길이
        String chars = "0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder authNumber = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length()); // 0~9 중 무작위 숫자 "1개"를 생성
            authNumber.append(chars.charAt(index)); // charAt은 문자열에서 특정 위치에 있는 '한 글자'를 가져옴 , append() 메서드는 문자열을 이어붙이지만 새 객체를 생성하지 않음,특히 반복문 안에서 문자열 조립할 때는 거의 필수
        }

        // 인증번호 저장
        verificationCache.put(userEmail, Map.of("authNumber", authNumber.toString(), "timestamp", System.currentTimeMillis()));

        // 인증번호 메일발송
        try {
            param.put("mailReceiver", userEmail+";"); // 세미콜론은 여러 명일 때 대비한 형식
            param.put("authNumber", authNumber);// 메일 내용에 포함될 인증번호
            fnSendMail(param);
        } catch (Exception e) {
            log.error("emailCheckProcess >>> fnSendMail : {}", e.getMessage());
            errorResult.put("message", "[사용자 인증 안내 : RTPE108] <br> 이메일 전송 중 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        HashMap<String, Object> rtnResult = new HashMap<>();
        rtnResult.put("authNumber", authNumber);
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
        verificationCache.asMap().computeIfPresent(userEmail, (key, existingData) -> Map.of( // verificationCache은 사용자 이메일 → 인증번호 정보(Map) 형태로 저장된 캐시
                                                                                                                    // verificationCache.asMap()은 Caffeine 캐시를 일반 Map처럼 다루게 함,.computeIfPresent(...)은 Map 인터페이스이고, 해당 키(userEmail)가 존재할 경우에만 value(existingData)값 수정                 // Map.of(...)은 불변 Map이기 때문에 data.put(...)으로 수정할 수 없습니다. 인증번호 유효 시간 연장 시 computeIfPresent()로 새 Map을 만들어 넣는 이유임
                "authNumber", existingData.get("authNumber"), // 기존값 유지
                "timestamp", System.currentTimeMillis() // timestamp 값을 현재 시각으로 업데이트
        ));

        rtnResult.put("message", "시간이 연장되었습니다."); // 키 이름은 상관 x
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

        Map<String, Object> data = verificationCache.getIfPresent(userEmail);

        if (data == null) {
            errorResult.put("message", "인증번호를 확인할 수 없습니다. 다시 요청해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        long storedTime = (long) data.get("timestamp");
        long elapsedTime = System.currentTimeMillis() - storedTime;
        long maxTime = TimeUnit.MINUTES.toMillis(3); // 3분

        if (elapsedTime > maxTime) {
            verificationCache.invalidate(userEmail); // 만료된 인증번호 삭제
            errorResult.put("message", "인증번호 확인 시간이 초과하였습니다. 다시 요청해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        if (!authNumber.equals(data.get("authNumber"))) {
            errorResult.put("message", "인증번호가 일치하지 않습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

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
                    decodedCompanyNo = EncriptUtil.decryptAes256(companyNo);
                    StringBuffer sb = new StringBuffer(decodedCompanyNo);
                    sb.insert(3, '-');
                    sb.insert(6, '-');
                    rtnResult.replace("companyNo", sb.toString());
                }
            } catch (Exception e) {
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

        Map<String, Object> memberMap = new ObjectMapper().convertValue(memberModel, Map.class);
        Map<String, Object> companyMap = new ObjectMapper().convertValue(companyModel, Map.class);
        Map<String, Object> resultMap = memberMapper.findUserByUserUid(memberMap);
        if (!BCrypt.checkpw(memberMap.get("userPwd").toString(), resultMap.get("userPwd").toString())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }
        //사업자 번호 암호화
        try {
            String encodedCompanyNo = EncriptUtil.encryptAes256(companyModel.getCompanyNo());
            companyMap.put("companyNo", encodedCompanyNo);

            memberMap.put("modUserId", memberModel.getUserEmail());
            companyMap.put("modUserId", memberModel.getUserEmail());
        } catch (Exception e) {
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
     * 이메일 전송
     */

    private void fnSendMail(Map<String, Object> param) {
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage(); // 간단한 텍스트 메일을 생성하는 객체
            simpleMailMessage.setTo(param.get("userEmail").toString()); // 실 서비스 오픈시 주석해제
            //simpleMailMessage.setTo("noo0916@daum.net"); // 테스트용
            simpleMailMessage.setSubject("[HSYNC] 사용자 인증 안내");
            simpleMailMessage.setText("임시비밀번호 : " + param.get("authNumber").toString());
            javaMailSender.send(simpleMailMessage); // javaMailSender는 Spring에서 제공하는 메일 전송기

        }catch(Exception e) {
            log.error("fnSendMail : {}", e.getMessage());
            throw e; // 로그는 fnSendMail() 안에서 남기고, 실제 처리나 사용자에게 알림은 상위 메서드에서 하기 위해서, 예외를 다시 호출한 쪽으로 던짐 ,emailCheckProcess()의 catch 블록으로 예외가 "다시 올라감
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