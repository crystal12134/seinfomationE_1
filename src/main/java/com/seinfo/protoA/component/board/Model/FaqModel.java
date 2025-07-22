package com.seinfo.protoA.component.board.Model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
@ApiModel(value = "WebApi faq DTO", description = "faq DTO 객체")
public class FaqModel {

    @ApiModelProperty(name = "faqNo", value = "faq 번호")
    private String faqNo;

    @ApiModelProperty(name = "faqType", value = "faq 구분")
    private String faqType;

    @NotBlank(message = "제목을 입력해주세요.")
    @ApiModelProperty(name = "faqQuestion", value = "faq 제목")
    private String faqQuestion;

    @NotBlank(message = "내용을 입력해주세요.")
    @ApiModelProperty(name = "faqAnswer", value = "faq 내용")
    private String faqAnswer;
}
