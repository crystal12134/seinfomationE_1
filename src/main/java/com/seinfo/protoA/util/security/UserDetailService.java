package com.seinfo.protoA.util.security;

import java.util.Map;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.seinfo.protoA.component.main.mapper.LoginMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailService implements UserDetailsService {

	private final LoginMapper loginMapper;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		
		Map<String, Object> user = loginMapper.selectUserByEmail(email);
		if (user == null) {
			throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
		}
		
		String userEmail =  String.valueOf(user.get("userEmail"));
		String userPwd = String.valueOf(user.get("userPwd"));
		String userRole = String.valueOf(user.get("userRole"));
		
		return User.builder()
				.username(userEmail)
				.password(userPwd)
				.roles(userRole)
				.build();
	}
}
