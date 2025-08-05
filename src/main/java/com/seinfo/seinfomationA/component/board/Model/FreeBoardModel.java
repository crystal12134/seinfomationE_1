package com.seinfo.seinfomationA.component.board.Model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
@ApiModel(value = "WebApi freeBoard DTO", description = "자유게시판 DTO 객체")
public class FreeBoardModel {

    @ApiModelProperty(name = "freeBoardNo", value = "게시물 번호")
    private String freeBoardNo;

    @ApiModelProperty(name = "freeBoardType", value = "게시물 구분")
    private String freeBoardType;

    @NotBlank(message = "제목을 입력해주세요.")
    @ApiModelProperty(name = "freeBoardTitle", value = "게시물 제목")
    private String freeBoardTitle;

    @NotBlank(message = "내용을 입력해주세요.")
    @ApiModelProperty(name = "freeBoardContents", value = "게시물 내용")
    private String freeBoardContents;

    @ApiModelProperty(name = "fileGrpId", value = "파일 첨부시 Group Id")
    private String fileGrpId;

    @ApiModelProperty(name = "userType", value = "USER TYPE")
    private String userType;

    @ApiModelProperty(name = "adminYn", value = "ADMIN ROLE")
    private String adminYn;

    @ApiModelProperty(name = "modifyYn", value = "수정모드")
    private String modifyYn;
}
