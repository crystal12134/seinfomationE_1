package com.seinfo.seinfomationA.component.main.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import io.swagger.annotations.ApiModelProperty;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
public class LoginModel implements Serializable {
    private static  final long serialVersionUID = 1L;

    private String userUid;

    @NotNull(message = "사용자 계정을 입력하세요.")
    @Size(min = 4, max = 20, message = "사용자 계정 길이가 잘못 되었습니다.")
    @ApiModelProperty(name = "id", value = "사용자 계정", example = "testUser", allowEmptyValue = false )
    private String id;

    @NotNull(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 20, message = "비밀번호 길이가 잘못 되었습니다.")
    @ApiModelProperty(name = "password", value = "사용자 비밀번호", example = "P@ssw0rd", allowEmptyValue = false )
    private String password;

    @NotNull(message = "새 비밀번호를 입력하세요.")
    @Size(min = 8, max = 20, message = "비밀번호 길이가 잘못 되었습니다.")
    @ApiModelProperty(name = "password", value = "사용자 비밀번호", example = "P@ssw0rd", allowEmptyValue = false )
    private String newPassword;

    private String type;
    private String lang;
    private String returnUrl;
    private String myInfoYn; //마이페이지 비밀번호 변경 여부
}
