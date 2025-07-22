package com.seinfo.protoA.component.member.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
@ApiModel(value = "WebApi Member DTO", description = "멤버 DTO 객체")
public class MemberModel implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int userUid;
	
	@NotBlank(message = "사용자 계정을 입력해 주세요.")
	@Email(message = "회원 아이디는 이메일 형식이 되어야 합니다.")
	@ApiModelProperty(name = "userEmail",value = "사용자 ID")
	private String userEmail;
	
	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Size(min = 8, max = 20, message = "비밀번호 길이는 최소 8자, 최대 20자 입니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[!@#$%^&*(),.?\":{}|<>])(?=.*\\d).{8,}$", message = "비밀번호는 영문, 특수문자, 숫자를 모두 하나이상 씩 포함해야 합니다.")
	@ApiModelProperty(name = "userPwd",value = "사용자 비밀번호")
	private String userPwd;
	
	@NotBlank(message= "회원 성명을 입력해주세요.")
	@ApiModelProperty(name = "userNm",value = "사용자 이름")
	private String userNm;
	
	@NotBlank(message = "사용자의 핸드폰 번호를 입력해 주세요.")
	@ApiModelProperty(name = "userPhone",value = "사용자 모바일 번호")
	private String userPhone;

	@ApiModelProperty(name = "userOfficePhone",value = "사용자 사무실 번호")
	private String userOfficePhone;

}
