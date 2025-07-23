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
@ApiModel(value = "WebApi QNA DTO", description = "QNA DTO 객체")
public class QnaModel {

    @ApiModelProperty(name = "noticeNo", value = "게시물 번호")
    private String noticeNo;

    @ApiModelProperty(name = "noticeType", value = "게시물 구분")
    private String noticeType;

    @NotBlank(message = "제목을 입력해주세요.")
    @ApiModelProperty(name = "noticeTitle", value = "질의 제목")
    private String noticeTitle;

    @NotBlank(message = "내용을 입력해주세요.")
    @ApiModelProperty(name = "noticeContents", value = "질의 내용")
    private String noticeContents;

    @ApiModelProperty(name = "answerTitle", value = "답변 제목")
    private String answerTitle;

    @ApiModelProperty(name = "answerContents", value = "답변 내용")
    private String answerContents;

    @ApiModelProperty(name = "userType", value = "USER TYPE")
    private String userType;

    @ApiModelProperty(name = "adminYn", value = "ADMIN ROLE")
    private String adminYn;

    @ApiModelProperty(name = "modifyMode", value = "수정모드")
    private String modifyMode;

    @ApiModelProperty(name = "qnaDelYn", value = "질의 삭제 여부")
    private String qnaDelYn;

    @ApiModelProperty(name = "replyYn", value = "답변 작성 여부")
    private String replyYn;
}
