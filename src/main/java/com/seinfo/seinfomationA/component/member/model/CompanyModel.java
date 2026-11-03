package com.seinfo.seinfomationA.component.member.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
@ApiModel(value = "WebApi Company DTO", description = "회사 DTO 객체")
public class CompanyModel implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@NotBlank(message = "사업자 번호를 입력해 주세요.")
	@Size(min=10, message="사업자 등록번호는 10자리가 되어야 합니다.")
	@ApiModelProperty(name = "companyNo",value="사업자번호")
	private String companyNo;
	
	@NotBlank(message = "사업자 이름를 입력해 주세요.")
	@ApiModelProperty(name = "companyNm",value="사업자이름")
	private String companyNm;

	@ApiModelProperty(name = "companyD",value="개업연원일")
	private String companyD;
	
	@NotBlank(message = "회사주소를 입력해 주세요.")
	@ApiModelProperty(name = "companyAddress",value="회사주소")
	private String companyAddress;

	@ApiModelProperty(name = "companyAddressDetail",value="회사주소 상세")
	private String companyAddressDetail;
	
	@NotBlank(message = "회사대표 이름을 입력해 주세요.")
	@ApiModelProperty(name = "ceoNm",value="회사대표이름")
	private String ceoNm;

	@ApiModelProperty(name = "ceoEmail",value="회사 대표 전화")
	private String ceoPhone;

	@ApiModelProperty(name = "ceoEmail ",value="회사대표 이메일")
	private String ceoEmail;

	@ApiModelProperty(name = "companyUrl",value="회사 URL")
	private String companyUrl;

	@ApiModelProperty(name = "companyInfo",value="회사소개")
	private String companyInfo ;
}
