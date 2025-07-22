package com.seinfo.protoA.component.member.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface MemberMapper {
    Map<String, Object> chkCompanyNoDuplicate(String companyNo); // 사업자번호 중복 여부

    int chkUserEmailDuplicate(Map<String, Object> map); // 이메일 중복 여부

    boolean insertVendorManager(Map<String, Object> map); // VenderManager 데이터 삽입

    boolean insertVendor(Map<String, Object> map); // Vender 데이터 삽입

    void updateUserIfResult(Map<String, Object> map); // Vender 인터페이스 성공 여부 갱신

    void updateVendorIfResult(Map<String, Object> map); // VenderManager인터페이스 성공 여부 갱신

    Map<String, Object> selectUsrAndCompanyInfo(Map<String, Object> map); // 회원정보 조회

    Map<String, Object> findUserByUserUid(Map<String, Object> map); // 패스워드 확인

    boolean updateUserInfo(Map<String, Object> map); // 회원정보 업데이트 - VenderManger 업데이트

    boolean updateVendorInfo(Map<String, Object> map); // 회원정보 업데이트 - Vender 업데이트

    Map<String, Object> selectUserHistory(Map<String, Object> param); // 회원 히스토리


}
