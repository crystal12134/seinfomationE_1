package com.seinfo.protoA.component.member.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.seinfo.protoA.component.main.mapper.LoginMapper;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
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

import com.seinfo.protoA.component.member.model.CompanyModel;
import com.seinfo.protoA.component.member.model.MemberModel;
import com.seinfo.protoA.component.member.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

@Controller
@RequiredArgsConstructor //  즉, 생성자 없이도 의존성 주입됨.
@RequestMapping("/anyone/member")
@Slf4j
public class AnyMemberController {

	private final MemberService memberService;
	private final ResponseEntityUtil responseEntityUtil;
	private final HttpSession session;

	@Value("${seinfo.openApi.address.url}") // 값 주입,${...} 안에 있는 키를 설정 파일에서 찾아서 해당 변수 삽입,외부 설정 파일(application.properties)
	private String addressUrl;

	@Value("${seinfo.openApi.address.confmKey}") // API 호출 시 사용되는 인증키
	private String confmKey;

	@Value("${server.domain}") //리다이렉션이나 외부 API 응답 시 사용
	private String returnUrl;

	/* 페이지 호출: 회원가입 - 이용약관*/
	@GetMapping(value = "/joinMember")
	public ModelAndView joinMemberPage() {

		ModelAndView mv = new ModelAndView(); // 웹페이지 정보를 담는 객체 생성,어떤 화면(View)을 보여줄지와 화면에 전달할 데이터를 함께 담는 객체

		session.setAttribute("joinStep", 1);
		mv.setViewName("seinfo/member/joinMember");
		return mv;

	}

	/*페이지 호출: 회원가입 - 사업자 인증*/
	@GetMapping(value = "/regMember")
	public ModelAndView regMemberPage() {

		ModelAndView mv = new ModelAndView();
		mv.setViewName("seinfo/member/regMember");

		Integer step = (Integer) session.getAttribute("joinStep"); //session.getAttribute()는 Object 타입을 반환,그래서 int로 바로 받을 수 없고, 객체형인 Integer로 형변환,int는 기본형이라 null을 저장할 수 없음.
		// 반면 Integer는 객체라서 null을 저장하거나 비교 가능.이 코드에서 step == null을 체크하는 부분→ 그래서 Integer가 필요
		if(step == null || (step !=2 && step !=3)) {
			mv.addObject("message", "비정상적인 경로로 접근하셨습니다.<br> 정상적인 경로를 통해 진행 바랍니다."); // addObject는 ModelAndView 포함된 메서드,addObject()는 값을 "얻어오는" 게 아니라 "세팅
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
	@PostMapping(value = "/regMember/checkBizNumber") // "사업자번호 확인" 버튼을 눌렀을 때 호출되는 메서드
	public ResponseEntity<?> checkBizNumber(@RequestBody Map<String, Object> param) {// @RequestBody는 JSON 데이터를 param이라는 맵으로 받음
		Map<String, Object> errorResult = new HashMap<String, Object>();

		String companyNo = Objects.toString(param.get("companyNo"),""); //일반적으로 obj.toString()을 직접 호출하면 → obj가 null일 경우 NullPointerException(NPE)이 발생, but 무조건 발생하는게 아니라 null에 대해 메서드 (toString(), equals(), 등)를 호출하면 위험
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
	public ModelAndView searchAddress() { // 주소 검색" 버튼을 클릭했을 때 호출
		ModelAndView mv = new ModelAndView();
		mv.addObject("addressUrl", addressUrl);
		mv.addObject("confmKey", confmKey); // API 인증키
		mv.addObject("returnUrl", returnUrl); // 팝업 닫은 후 돌아올 URL
		mv.setViewName("seinfo/member/popupAddress");

		return mv;
	}

	@PostMapping(value = "/regMember/AddressPopup")
	public ModelAndView searchAddressResult(HttpServletRequest request) { // 외부 주소 검색 팝업에서 주소를 선택하고 나서 우리 쪽 서버로 POST 요청이 들어올 때 호출되는 메서드
																			// HttpServletRequest는 클라이언트(브라우저)가 서버에 보낸 모든 요청 정보를 담고 있는 자바 객체, HTML form에서 input name의 value 값들을 받아옴
		ModelAndView mv = new ModelAndView();
		mv.setViewName("seinfo/member/popupAddress"); // 1. HTML에서 외부 주소 API를 JavaScript로 호출 ,2 사용자가 주소 선택하고 확인 → input 값들 채워짐, 3 form 제출 (POST 방식), 4.서버에서 컨트롤러가 해당 값들 받아옴

		//폼에서 전송된 값들을 하나씩 꺼내는 방법
		String inputYn = request.getParameter("inputYn"); //폼에서 전송된 값들을 하나씩 꺼내는 방법,<input name="키">에서 넘어온 값
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

	/*(from) 사업자인증 페이지에서 (to) 사용자 인증 페이지도 data 전송 // 이렇게 하면 **다음 단계(사용자 인증 페이지)**에서 이 정보를 다시 사용할 수 있다.
	예를 들어 다음 페이지에서 회사명, 대표자명을 보여준다거나 내부 로직에서 참조할 때 사용
	 * @param companyModel
	 * @return redirectAttributes*/

	@PostMapping(value = "/regMember/companyProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> companyProcess(@RequestBody @Valid CompanyModel companyModel, BindingResult bindingResult) { // @RequestBody: 요청 바디(JSON)에 담긴 값을 CompanyModel 객체로 자동 매핑
																														  // @Valid: 이 모델에 정의된 유효성 검사 조건을 자동으로 체크 (예: @NotBlank, @Size)
		Map<String, Object> errorResult = new HashMap<String,Object>();

		if(bindingResult.hasErrors()) { // @Valid로 검증된 객체에 오류가 있는지 확인 , 필수값이 비어 있거나 형식이 틀렸을 때 → true(오류) 반환
			FieldError fieldError = bindingResult.getFieldError(); // 오류 내용은 bindingResult.getFieldError(), getFieldError()메서드의 반환 타입이 FieldError이기떄문에 FieldError 객체 담는다

			if(fieldError != null) {
				String errorMessage = fieldError.getDefaultMessage();

				errorResult.put("message", errorMessage);
			}
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}
		session.setAttribute("joinStep", 3); // "joinStep"을 3으로 저장함으로서 3단계: 사용자 인증 페이지 진입 가능)
		session.setAttribute("companyModel", companyModel); // 사용자가 입력한 사업자 정보(CompanyModel)를 세션에 저장 → 다음 단계 페이지에서 이 정보 활용 가능
		return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"E001","정상적으로 처리 됐습니다.");
	}

	/*사용자 인증: 메일발송
	 * @param userId
	 * @return*/
	@PostMapping("/usrMember/sendAuthMail")
	public ResponseEntity<?> sendAuthMail(@RequestBody Map<String, Object> param) { // JSON 형태로 들어온 요청 데이터를 param이라는 Map으로 받음
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
	public ResponseEntity<?> authNumberChk(@RequestBody Map<String, Object> param) { //  "인증번호"를 입력하고 [확인] 버튼을 눌렀을 때 실행
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
	public ResponseEntity<?> extendTime(@RequestBody Map<String, Object> param) { // 인증시간 연장" 버튼을 눌렀을 때 실행
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
		CompanyModel companyModel = (CompanyModel) session.getAttribute("companyModel"); // 이전 단계(사업자 인증)에서 세션에 저장된 companyModel을 꺼냄

		if(bindingResult.hasErrors()) {
			FieldError fieldError = bindingResult.getFieldError();
			if(fieldError != null) {
				errorResult.put("message", fieldError.getDefaultMessage());
			}
			return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED,"E001",errorResult);
		}
		try {
			memberService.setMemberInfoAdd(memberModel, companyModel); // 사용자 정보 + 사업자 정보를 memberService에서 DB에 저장
			session.removeAttribute("companyModel"); // 회원가입 완료 후에는 세션 정보삭제 근데 왜 이것만? , 이전 단계인 companyProcess()에서 중간 상태로 서버에 저장해둔 객체, 이전 사용자의 정보가 남아있을 위험
															// memberModel은 세션에 저장된 적이 없으므로 제거대상 x
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
		session.setAttribute("joinStep", param.get("joinStep")); // null 저장만 하는 건 OK , but null인 값을 .toString()이나 .equals() 등 메서드 호출하려고 한다면 NEP발생, get 메서드는 괜찮음 단지 이걸가지고 뭘 호출하려고하면 위험하다.
		return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS,"S001",Map.of("success","success"));
	}
}

	