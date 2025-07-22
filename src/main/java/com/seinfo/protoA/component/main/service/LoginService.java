package com.seinfo.protoA.component.main.service;

import java.util.Map;

//import javax.servlet.http.HttpServletRequest; //직접입력했는데 여기서는 필요없는건지? javax=>jakarta 라고 했는데 맞는지?
//import javax.servlet.http.HttpServletResponse;  //직접입력했는데 여기서는 필요없는건지? javax=>jakarta 라고 했는데 맞는지?
//import jakarta.servlet.http.HttpServletRequest; //사용안함.내가 추가한것-사용되지 않는 import 문이라고 경고 메시지 나옴 삭제해도 되는건지?
//import jakarta.servlet.http.HttpServletResponse;//사용안함.내가 추가한것-사용되지 않는 import 문이라고 경고 메시지 나옴 삭제해도 되는건지?

import org.springframework.http.ResponseEntity;
//import org.springframework.web.servlet.ModelAndView; //사용안함.직접입력했는데 여기서는 필요없는건지?
//import pf.core.web.api.model.CoreWebApiResponseModel; //사용안함.직접입력-사용되지 않는 import 문이라고 경고 메시지 나옴 삭제해도 되는건지?
import com.seinfo.protoA.component.main.model.LoginModel;

public interface LoginService {

    ResponseEntity<?> loginProcess(LoginModel loginModel) throws Exception; // 로그인

    ResponseEntity<?> removeToken() throws Exception; //로그아웃

    ResponseEntity<?> unlockPwdProcess(Map<String, Object> param) throws Exception; // 임시 비밀번호 발급

    ResponseEntity<?> changePwdProcess(LoginModel loginModel) throws Exception; // 비민번호 변경

    ResponseEntity<?> pwdFindProcess(Map<String, Object> param) throws Exception; // 비밀번호 찾기

    ResponseEntity<?> idFindProcess(Map<String, Object> param); // id 찾기

    ResponseEntity<?> refreshToken() throws Exception;
}
