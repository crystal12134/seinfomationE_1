package com.seinfo.seinfomationA.component.member.service;

import com.seinfo.seinfomationA.component.member.model.CompanyModel;
import com.seinfo.seinfomationA.component.member.model.MemberModel;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface MemberService {
	
	void setMemberInfoAdd(MemberModel memberModel, CompanyModel companyModel); // 회원가입

	ResponseEntity<?> checkBizNumberProcess(String companyNo, String ceoNm, String companyD, String companyNm);// 사업자 인증

	ResponseEntity<?> emailCheckProcess(String userId); // 이메일 인증

	public ResponseEntity<?> extendTime(String userEmail);

	ResponseEntity<?> checkAuthNumberProcess(String email, String authNumber); // 인증번호 확인

	ResponseEntity<?> getMemberInfo(); // 회원정보 보기

	void setMyInfoUpd(MemberModel memberModel, CompanyModel companyModel); // 회원정보 업데이트

	ResponseEntity<?> dropUserInfo(Map<String, Object> param) throws Exception; //회원탈퇴

}
