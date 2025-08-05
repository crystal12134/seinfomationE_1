package com.seinfo.seinfomationA.component.request.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Generated;

@Data
@Generated
@ApiModel(value = "WebApi request DTO", description = "의뢰 DTO 객체")
public class RequestModel {
    private static final long serialVersionUID = 1L;

    private String requestNo;

    @NotBlank(message = "제안 제목을 입력해 주세요.")
    @ApiModelProperty(name = "requestTitle", value = "의뢰 제목")
    private String requestTitle;

    @NotBlank(message = "제안 내용을 입력해주세요.")
    @ApiModelProperty(name = "proposalDetail", value = "의뢰 내용")
    private String requestDetail;

    @ApiModelProperty(name = "fileGrpId", value = "파일 첨부시 Group Id")
    private String fileGrpId;

    private String sendD; // 제출일자

    private String requestState; // 요청 상태
    // 01 : 작성중, 10 : 제출, 20 : 진행 중, 99 : 의뢰 완료

    private String requestSeq;
    private String companyNm; // 제안 회사명
}
