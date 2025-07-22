package com.seinfo.protoA.util.response;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
@RequiredArgsConstructor
public class ResponseEntityUtil {

	private final HttpServletRequest req;
	private final String httpUidPrefix = "res" + (new SimpleDateFormat("-yyyyMMdd-").format(new Date())) ;

    public ResponseEntity<CoreWebApiResponseModel> getResponseEntity(CodeCoreResultList processCode) {
        String httpMsg = this.req.getRequestURI() + " called";

        return ResponseEntity.ok(CoreWebApiResponseModel.builder()
                .http_uid(this.httpUidPrefix + this.httpUidPrefix)
                .http_msg(httpMsg)
                .timestamp(new Date())
                .process_code(processCode.getCode())
                .process_status(processCode.getValue())
                .process_msg("")
                .data("").build());
    }
	
	public ResponseEntity<CoreWebApiResponseModel> getResponseEntity(CodeCoreResultList processCode, String resultMsg){
        String httpMsg = this.req.getRequestURI() + " called";

        return ResponseEntity.ok(CoreWebApiResponseModel.builder()
                .http_uid(this.httpUidPrefix + this.httpUidPrefix)
                .http_msg(httpMsg)
                .timestamp(new Date())
                .process_code(processCode.getCode())
                .process_status(processCode.getValue())
                .process_msg(resultMsg)
                .data("").build());
	}

    public ResponseEntity<CoreWebApiResponseModel> getResponseEntity(CodeCoreResultList processCode, String resultMsg, Object data){
        String httpMsg = this.req.getRequestURI() + " called";

        return ResponseEntity.ok(CoreWebApiResponseModel.builder()
                .http_uid(this.httpUidPrefix + this.httpUidPrefix)
                .http_msg(httpMsg)
                .timestamp(new Date())
                .process_code(processCode.getCode())
                .process_status(processCode.getValue())
                .process_msg(resultMsg)
                .data(data).build());
    }

    public ResponseEntity<CoreWebApiResponseModel> getResponseEntity(CodeCoreResultList processCode, String resultMsg, Object data, Exception e){
        String httpMsg = this.req.getRequestURI() + " called";

        return ResponseEntity.ok(CoreWebApiResponseModel.builder()
                .http_uid(this.httpUidPrefix + this.httpUidPrefix)
                .http_msg(httpMsg)
                .timestamp(new Date())
                .process_code(processCode.getCode())
                .process_status(processCode.getValue())
                .process_msg(resultMsg == null || resultMsg.isEmpty() ? e.getMessage() : resultMsg)
                .data(data).build());
    }


    public ResponseEntity<CoreWebApiResponseModel> getResponseEntity(CodeCoreResultList processCode, String resultMsg, Object data, Throwable e){
        String httpMsg = this.req.getRequestURI() + " called";

        return ResponseEntity.ok(CoreWebApiResponseModel.builder()
                .http_uid(this.httpUidPrefix + this.httpUidPrefix)
                .http_msg(httpMsg)
                .timestamp(new Date())
                .process_code(processCode.getCode())
                .process_status(processCode.getValue())
                .process_msg(resultMsg == null || resultMsg.isEmpty() ? e.getMessage() : resultMsg)
                .data(data).build());
    }

    public ResponseEntity<CoreWebApiResponseModel> getResponseEntity(Map<String, Object> errorAtr){
        HttpStatus httpStatus = HttpStatus.valueOf(Integer.parseInt(errorAtr.get("status").toString()));

        return new ResponseEntity<>(CoreWebApiResponseModel.builder()
                .http_uid(this.httpUidPrefix + this.httpUidPrefix)
                .http_msg((errorAtr.get("path") != null ? errorAtr.get("path").toString() : "") + " called")
                .timestamp(new Date())
                .process_code(CodeCoreResultList.RESULT_CODE_FAILED.getCode())
                .process_status(CodeCoreResultList.RESULT_CODE_FAILED.getValue())
                .process_msg(errorAtr.get("message") != null ? errorAtr.get("message").toString() : "")
                .data("").build(), httpStatus);
    }

}
