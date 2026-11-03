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

    @ApiModelProperty(name = "freeBoardNo", value = "자유게시판 번호")
    private String freeBoardNo;

    @ApiModelProperty(name = "freeBoardType", value = "자유게시판 구분")
    private String freeBoardType;

    @NotBlank(message = "제목을 입력해주세요.")
    @ApiModelProperty(name = "freeBoardTitle", value = "자유게시판 제목")
    private String freeBoardTitle;

    @NotBlank(message = "내용을 입력해주세요.")
    @ApiModelProperty(name = "freeBoardContent", value = "자유게시판 내용")
    private String freeBoardContent;

    @ApiModelProperty(name = "modifyYn", value = "자유게시판 글 수정모드")
    private String modifyYn;

    @ApiModelProperty(name = "likeCount", value = "자유게시판 상세페이지 추천 수")
    private int likeCount;

    @ApiModelProperty(name = "hateCount", value = "자유게시판 상세페이지 비추천 수")
    private int hateCount;

    // --- Getter/Setter 추가 ---
    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getHateCount() {
        return hateCount;
    }

    public void setHateCount(int hateCount) {
        this.hateCount = hateCount;
    }                                       
}
