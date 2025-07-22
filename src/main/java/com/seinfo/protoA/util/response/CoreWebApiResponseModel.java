package com.seinfo.protoA.util.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@ApiModel(value = "webApi Response DTO")
@Setter
@Getter
@Builder
public class CoreWebApiResponseModel {

    private static final long serialVersionUID = 1L;

    @NotNull
    @ApiModelProperty(name = "http_uid", value="HTTP 요청번호", position = 0, allowEmptyValue = false)
    private String http_uid;

    @NotNull
    @ApiModelProperty(name = "http_msg", value="HTTP 요청 URI", position = 3, allowEmptyValue = false)
    private String http_msg;

    @NotNull
    @ApiModelProperty(name = "timestamp", value="처리시간", position = 4, allowEmptyValue = true)
    private Date timestamp;

    @NotNull
    @ApiModelProperty(name = "process_code", value="처리 코드: {성공[00], 실패[01]}", position = 5, allowEmptyValue = true)
    private String process_code;

    @NotNull
    @ApiModelProperty(name = "process_status", value="처리 상태값: {SUCCESS, FAILED}", position = 6, allowEmptyValue = true)
    private String process_status;

    @NotNull
    @ApiModelProperty(name = "process_msg", value="처리 메세지", position = 7, allowEmptyValue = true)
    private String process_msg;

    @NotNull
    @ApiModelProperty(name = "data", value="처리 값", position = 8, allowEmptyValue = true)
    private transient Object data;

    public CoreWebApiResponseModel(@NotNull  String http_uid, @NotNull String http_msg, @NotNull Date timestamp, @NotNull String process_code, @NotNull String process_status, String process_msg, Object data) {
        this.http_uid = http_uid;
        this.http_msg = http_msg;
        this.timestamp = timestamp;
        this.process_code = process_code;
        this.process_status = process_status;
        this.process_msg = process_msg;
        this.data = data;
    }
}
