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
public class TeamModel {

    @ApiModelProperty(name = "teamNo", value = "teamNo")
    private int teamNo;

    @ApiModelProperty(name = "teamNm", value = "팀명")
    private String teamNm;

    @ApiModelProperty(name = "teamManager", value = "팀장")
    private String teamManager;

    @ApiModelProperty(name = "teamStartDt", value = "시작일")
    private String teamStartDt;

    @ApiModelProperty(name = "teamEndDt", value = "종료일")
    private String teamEndDt;

    @ApiModelProperty(name = "useYn", value = "사용여부")
    private String useYn;

    @ApiModelProperty(name = "regUserId", value = "등록자")
    private String regUserId;
}
