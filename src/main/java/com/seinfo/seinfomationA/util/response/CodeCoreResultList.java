package com.seinfo.seinfomationA.util.response;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum CodeCoreResultList {
	RESULT_CODE_SUCCESS("SUCCESS", "00"),
	RESULT_CODE_FAILED("FAILED", "01"),
	LIST(
			Arrays.asList(new CodeCoreResultList[] { RESULT_CODE_SUCCESS, RESULT_CODE_FAILED})
	);
	
	private String code;
	private String value;
	private List<CodeCoreResultList> dataList;
	
	CodeCoreResultList(String value, String code) {
		this.value = value;
		this.code = code;
	}

	CodeCoreResultList(List<CodeCoreResultList> dataList) {
		this.dataList = dataList;
	}

}
