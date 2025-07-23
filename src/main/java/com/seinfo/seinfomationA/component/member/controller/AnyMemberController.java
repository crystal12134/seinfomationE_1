package com.seinfo.seinfomationA.component.member.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.seinfo.seinfomationA.component.member.model.CompanyModel;
import com.seinfo.seinfomationA.component.member.model.MemberModel;
import com.seinfo.seinfomationA.component.member.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor //  즉, 생성자 없이도 의존성 주입됨.
@RequestMapping("/anyone/member")
@Slf4j
public class AnyMemberController {

	private final MemberService memberService;
	private final ResponseEntityUtil responseEntityUtil;
	private final HttpSession session;

	@Value("${seinfo.openApi.address.url}")
	private String addressUrl;

	@Value("${seinfo.openApi.address.confmKey}")
	private String confmKey;

	@Value("${server.domain}")
	private String returnUrl;

	/* 페이지 호출: 회원가입 - 이용약관*/
	@GetMapping(value = "/joinMember")
	public ModelAndView joinMemberPage() {

		ModelAndView mv = new ModelAndView();

		session.setAttribute("joinStep", 1);
		mv.setViewName("seinfo/member/joinMember");
		return mv;

	}

	/*페이지 호출: 회원가입 - 사업자 인증*/
	@GetMapping(value = "/regMember")
	public ModelAndView regMemberPage() {

		ModelAndView mv = new ModelAndView();
		mv.setViewName("seinfo/member/regMember");

		Integer step = (Integer) session.getAttribute("joinStep");
		if(step == null || (step !=2 && step !=3)) {
			mv.addObject("message", "비정상적인 경로로 접근하셨습니다.<br> 정상적인 경로를 통해 진행 바랍니다.");
			return mv;
		}

		mv.addObject("joinStep", step);
		return mv;
	}

	/*페이지 호출: 회원가입 - 사용자인증*/
	@GetMapping(value = "/usrMember")
	public ModelAndView usrMemberPage() {

		ModelAndView mv = new ModelAndView();
		mv.setViewName("seinfo/member/usrMember");

		Integer step = (Integer) session.getAttribute("joinStep");
		if(step == null || step !=3) {

			mv.addObject("joinStep", step);
			mv.addObject("message", "비정상적인 경로로 접근하셨습니다.<br/> 정상적인 경로를 통해 진행 바랍니다.");
			return mv;
		}

		return mv;
	}

	/*페이지호출: 회원가입 - 가입완료*/
	@GetMapping(value = "/endMember")
	public ModelAndView endMemberPage() {

		ModelAndView mv = new ModelAndView();
		mv.setViewName("seinfo/member/endMember");

		Integer step = (Integer) session.getAttribute("joinStep");
		if(step == null || step !=4) {
			mv.addObject("joinStep", step);
			mv.addObject("message", "비정상적인 경로로 접근하셨습니다<br/> 정상적인 경로를 통해 진행바랍니다.");
			return mv;
		}
		session.invalidate(); // 세션 초기화
		return mv;
	}

	/*사업자인증 API
	 * @companyModel
	 * return ResponseEntity*/
	@PostMapping(value = "/regMember/checkBizNumber")
	public ResponseEntity<?> checkBizNumber(@RequestBody Map<String, Object> param) {
		Map<String, Object> errorResult = new HashMap<String, Object>();

		String companyNo = Objects.toString(param.get("companyNo"),"");
		if(companyNo.isBlank()) {
			errorResult.put("message", "사업자 번호가 비어 있습니다.");
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}
		//log.info("이름: {}", user.getName());
		String ceoNm = Objects.toString(param.get("ceoNm"),"");
		if(ceoNm.isBlank()) {
			errorResult.put("message", "대표자 성명이 비어 있습니다.");
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}

		String companyD = Objects.toString(param.get("companyD"),"");
		if(companyD.isBlank()) {
			errorResult.put("message", "개업연원일이 비어 있습니다.");
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}
		companyD = companyD.replace("-", ""); // 개업일 문자열에서 -를 제거 → 예: "2023-01-01" → "20230101"

		String companyNm = Objects.toString(param.get("companyNm"),"");
		if(companyNm.isBlank()) {
			errorResult.put("message", "회사명이 비어 있습니다.");
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}

		return memberService.checkBizNumberProcess(companyNo,ceoNm,companyD,companyNm);
	}

	/*주소검색 API (팝업 API)
	 * @param
	 * return ModelAndView*/

	@GetMapping(value = "/regMember/AddressPopup")
	public ModelAndView searchAddress() {
		ModelAndView mv = new ModelAndView();
		mv.addObject("addressUrl", addressUrl);
		mv.addObject("confmKey", confmKey);
		mv.addObject("returnUrl", returnUrl);
		mv.setViewName("seinfo/member/popupAddress");

		return mv;
	}

	@PostMapping(value = "/regMember/AddressPopup")
	public ModelAndView searchAddressResult(HttpServletRequest request) {

		ModelAndView mv = new ModelAndView();
		mv.setViewName("seinfo/member/popupAddress");

		//폼에서 전송된 값들을 하나씩 꺼내는 방법
		String inputYn = request.getParameter("inputYn");
		String zipNo = request.getParameter("zipNo");
		String roadAddrPart1 = request.getParameter("roadAddrPart1");
		String roadAddrPart2 = request.getParameter("roadAddrPart2");
		String addrDetail = request.getParameter("addrDetail");
		String jibunAddr = request.getParameter("jibunAddr");

		//컨트롤러에서 받은 값을 뷰(HTML) 쪽으로 넘기는 단계입니다.
		mv.addObject("inputYn", inputYn);
		mv.addObject("zipNo", zipNo);
		mv.addObject("roadAddrPart1", roadAddrPart1);
		mv.addObject("roadAddrPart2", roadAddrPart2);
		mv.addObject("jibunAddr", jibunAddr);
		mv.addObject("addrDetail", addrDetail);

		return mv;

	}

	@PostMapping(value = "/regMember/companyProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> companyProcess(@RequestBody @Valid CompanyModel companyModel, BindingResult bindingResult) {

		Map<String, Object> errorResult = new HashMap<String,Object>();

		if(bindingResult.hasErrors()) {
			FieldError fieldError = bindingResult.getFieldError();

			if(fieldError != null) {
				String errorMessage = fieldError.getDefaultMessage();

				errorResult.put("message", errorMessage);
			}
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}
		session.setAttribute("joinStep", 3);
		session.setAttribute("companyModel", companyModel);
		return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"E001","정상적으로 처리 됐습니다.");
	}

	/*사용자 인증: 메일발송
	 * @param userId
	 * @return*/
	@PostMapping("/usrMember/sendAuthMail")
	public ResponseEntity<?> sendAuthMail(@RequestBody Map<String, Object> param) {
		String userEmail = Objects.toString(param.get("userEmail"),"");
		if(userEmail.isBlank()) {
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",Map.of("message","회원 아이디가 비어 있습니다."));
		}

		return memberService.emailCheckProcess(userEmail);
	}

	/*사용자인증: 확인
	 * @userId
	 *@return */

	@PostMapping("/usrMember/authNumberChk")
	public ResponseEntity<?> authNumberChk(@RequestBody Map<String, Object> param) {
		String userEmail = Objects.toString(param.get("userEmail"),"");
		String authNumber = Objects.toString(param.get("authNumber"),"");
		if(authNumber.isBlank()) {
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",Map.of("message","인증번호가 일치하지 않습니다."));
		}

		return memberService.checkAuthNumberProcess(userEmail,authNumber);
	}

	/*사용자인증: 시간연장
	 * @userId
	 *@return */
	@PostMapping("/usrMember/extendTime")
	public ResponseEntity<?> extendTime(@RequestBody Map<String, Object> param) {
		String userEmail = Objects.toString(param.get("userEmail"),"");
		return memberService.extendTime(userEmail);
	}

	/*회원가입 정보 저장
	 * @param
	 *@return
	 *throws Exception */
	@PostMapping(value = "/usrMember/addMemberInfo")
	public ResponseEntity<?> memberInfoAdd(@RequestBody @Valid MemberModel memberModel, BindingResult bindingResult) throws Exception{
		Map<String, String> errorResult = new HashMap<>();
		CompanyModel companyModel = (CompanyModel) session.getAttribute("companyModel");

		if(bindingResult.hasErrors()) {
			FieldError fieldError = bindingResult.getFieldError();
			if(fieldError != null) {
				errorResult.put("message", fieldError.getDefaultMessage());
			}
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}
		try {
			memberService.setMemberInfoAdd(memberModel, companyModel);
			session.removeAttribute("companyModel");
			session.setAttribute("joinStep", 4);

			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001","회원가입 성공");

		} catch (DataAccessException e) {  // DB 관련 예외 처리
			log.error("setMemberInfoAdd >>> dataAccessException >>> 예외발생: {}", e.toString());
			errorResult.put("message", "예외발생 : 처리할 수 없습니다. (dataAccessException)");
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		} catch (Exception e) {	 // 그 외 모든 예외 처리
			errorResult.put("message", e.getMessage());
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}
	}

	/*회원단계 설정
	 * @param memberModel
	 * @return
	 * @throws Exception*/
	@PostMapping(value = "/setJoinStep")
	public ResponseEntity<?> setJoinStep(@RequestBody Map<String, Object> param) {
		session.setAttribute("joinStep", param.get("joinStep"));
		return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001",Map.of("success","success"));
	}
}

	