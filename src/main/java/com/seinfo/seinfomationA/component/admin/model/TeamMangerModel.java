package com.seinfo.seinfomationA.component.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
@ApiModel(value = "WebApi Team DTO", description = "팀원관리 DTO 객체")
public class TeamMangerModel {

    @ApiModelProperty(name = "userUid", value = "userUid")
    private int userUid;

    @ApiModelProperty(name = "userEmail", value = "이메일")
    private String userEmail;

    private String loginId;

    @ApiModelProperty(name = "userPhone", value = "모바일")
    private String userPhone;

    @ApiModelProperty(name = "userNm", value = "이름")
    private String userNm;

    @ApiModelProperty(name = "teamNo", value = "팀번호 -> 팀명")
    private String teamNo;

    @ApiModelProperty(name = "teamGrade", value = "직위")
    private String teamGrade;

    @ApiModelProperty(name = "regUserId", value = "등록자")
    private String regUserId;
}
