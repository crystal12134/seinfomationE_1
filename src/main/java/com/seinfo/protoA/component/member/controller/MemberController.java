package com.seinfo.protoA.component.member.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seinfo.protoA.component.main.model.LoginModel;
import com.seinfo.protoA.component.main.service.LoginService;
import com.seinfo.protoA.component.member.model.CompanyModel;
import com.seinfo.protoA.component.member.model.MemberModel;
import com.seinfo.protoA.component.member.service.MemberService;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final LoginService  loginService;
    private final ResponseEntityUtil responseEntityUtil;

    /**
     * 페이지호출 : 회원정보수정
     */
    @GetMapping(value = "/myInfo")
    public ModelAndView myInfoPage(){
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/member/myInfo");
        return mv;
    }

    /**
     * 페이지호출 : 탈퇴하기
     */
    @GetMapping(value = "/dropInfo")
    public ModelAndView dropInfoPage(){
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/member/dropInfo");
        return mv;
    }

    /**
     * 회원가입 정보 호출
     *
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/myInfo/getMemberInfo")

    public ResponseEntity<?> getMemberInfo(){
        return memberService.getMemberInfo();
    }

    /**
     * 회원정보 수정
     */
    @PutMapping(value = "/myInfo/myInfoUpd")
    public ResponseEntity<?> myInfoUpd(@RequestBody Map<String, Object> param) throws Exception{
        Map<String, Object> errorResult = new HashMap<String, Object>();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MemberModel memberModel = objectMapper.convertValue(param, MemberModel.class);
        CompanyModel companyModel = objectMapper.convertValue(param, CompanyModel.class);

        //Validation 검증 시작
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        //멤버 서버 내 검증
        Set<ConstraintViolation<MemberModel>> memberViolations = validator.validate(memberModel);
        memberViolations.removeIf(violation -> violation.getPropertyPath().toString().equals("userPwd")); //비밀번호 규칙 검증 무시

        if (!memberViolations.isEmpty()) {
            String firstError = memberViolations.stream()
                    .findFirst()
                    .map(ConstraintViolation::getMessage)
                    .orElse("잘못된 요청입니다.");

            errorResult.put("message", firstError);
            return ResponseEntity.internalServerError().body(errorResult);
        }

        //회사 서버 내 검증
        Set<ConstraintViolation<CompanyModel>> companyViolations = validator.validate(companyModel);
        if (!companyViolations.isEmpty()) {
            String firstError = companyViolations.stream()
                    .findFirst()
                    .map(ConstraintViolation::getMessage)
                    .orElse("잘못된 요청입니다.");

            errorResult.put("message", firstError);
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }

        try {
            memberService.setMyInfoUpd(memberModel, companyModel);
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "회원정보가 수정되었습니다.");
        } catch (BadCredentialsException e) {
            errorResult.put("message", e.getMessage());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        } catch (Exception e) {
            log.error("setMemberInfoUpd 예외 발생: {}", e.getMessage());
            errorResult.put("message", e.getMessage());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
    }

    /**
     * 비밀번호 변경 (회원정보수정 하단 팝업화면에서 사용자가 직접 변경시)
     */
    @PostMapping(value = "/myInfo/passChange/changePwdProcess")
    public ResponseEntity<?> psaaChangeProcess(@RequestBody LoginModel loginModel) throws Exception {
        return loginService.changePwdProcess(loginModel);
    }

    /**
     * 회원탈퇴
     */
    @PostMapping(value = "/dropInfo/userDrop")
    public ResponseEntity<?> dropUserInfo(@RequestBody Map<String, Object> param) throws Exception {
        return memberService.dropUserInfo(param);
    }
}
