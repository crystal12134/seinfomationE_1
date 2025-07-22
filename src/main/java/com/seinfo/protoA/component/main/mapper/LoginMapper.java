package com.seinfo.protoA.component.main.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginMapper {

	Map<String, Object> selectUsrInfo(Map<String, Object> tmp);

	Map<String, Object> selectUserByEmail(String email);

	void updateUsrTempPwd(Map<String, Object> param); // 임시비밀번호 발송

	void updateUsrPwd(Map<String, Object> param);

	void insertAccessLog(Map<String, Object> param);

	void updateUsrLoginFail(Map<String, Object> param); // 로그인 실패

	void updateUsrLoginSuccess(Map<String, Object> param); // 로그인 성공

	Map<String, Object> findUsrEmail(Map<String, Object> tmp); // 이메일 찾기

	void insertMail(Map<String, Object> map); // 메일 발송

	void insertMailVar(Map<String, Object> map); // 템플릿 내용 테이블

	Map<String, Object> findUsrInfoByUid(Map<String, Object> tmp);
}
