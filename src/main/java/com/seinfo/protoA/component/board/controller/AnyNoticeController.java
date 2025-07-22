package com.seinfo.protoA.component.board.controller;

import com.seinfo.protoA.component.file.service.TusService;
import com.seinfo.protoA.component.board.Model.NoticeModel;
import com.seinfo.protoA.component.board.service.NoticeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/anyone/noticeList")
@RequiredArgsConstructor
@Slf4j
public class AnyNoticeController {

    private final NoticeService noticeService;

    //공지사항 첨부파일 조회용 : 토큰없이 가능 (upd 2024.04.05)
    private final TusService tusService;

    /**
     * 페이지호출 : 공지사항 (외부:LRTP)
     */
    @GetMapping(value = "")
    public ModelAndView noticeBoardPage(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String searchType,
                                        @RequestParam(required = false) String noticeNo) {
        return noticeService.loadNoticeBoardPage(page, keyword, searchType, noticeNo);
    }

    /**
     * 공지사항 목록 조회
     */
    @PostMapping(value = "/reloadNoticeList")
    public ResponseEntity<?> reloadNoticeList(@RequestBody Map<String, Object> param) {
        return noticeService.reloadNoticeList(param);
    }

    /**
     * 공지사항 입력 : 팝업화면
     */
    @PostMapping(value = "/registNotice", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registNotice(@RequestBody NoticeModel noticeModel, BindingResult bindingResult) {
        Map<String, Object> message = new HashMap<String, Object>();

        if(bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if(fieldError != null) {
                String errorMessage = fieldError.getDefaultMessage();
                message.put("message", errorMessage);
            }
            return ResponseEntity.badRequest().body(message);
        }
        return noticeService.registNotice(noticeModel);
    }

    /**
     * 공지사항 상세조회 : 팝업화면
     */
    @PostMapping(value = "/getNoticeDetail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getNoticeDetail(@RequestBody Map<String, Object> param) {
        return noticeService.getNoticeDetail(param);
    }

    /**
     * 공지사항 삭제
     */
    @DeleteMapping(value = "/deleteNotice", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteNotice(@RequestBody Map<String, Object> param) throws Exception{
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        try {
            return noticeService.deleteNotice(param);
        }catch (Exception e) {
            rtnMap.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(rtnMap);
        }
    }

    //공지사항 첨부파일 조회용 : 토큰없이 가능
    @PostMapping(value = "/mfileGroupInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMfileGroupInfo(@RequestBody Map<String, Object> data) throws Exception {
        return tusService.getMfileGroupInfo(data);
    }

    //첨부팡리 목록 가져오기 - 공지사항 첨부파일 다운용 : 토큰없이 가능
    @PostMapping(value = "/mfilePermit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mfilePermit(@RequestBody Map<String, Object> data) throws Exception {
        return tusService.getMfileDownInfo(data);
    }

    //파일 다운로드 API - 공지사항 첨부파일 다운용 : 토큰없이 가능
    @GetMapping(path = "/filedownload")
    public ResponseEntity<?> getPrivateFileDown(HttpServletResponse response, @RequestParam String mfileDownId) throws Exception{
        Map<String, Object> param = new HashMap<>();
        param.put("mfileDownId", mfileDownId);
        return tusService.getFileDown(response, param);
    }
}


