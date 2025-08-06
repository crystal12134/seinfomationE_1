package com.seinfo.seinfomationA.component.board.controller;

import com.seinfo.seinfomationA.component.file.service.TusService;
import com.seinfo.seinfomationA.component.board.Model.FreeBoardModel;
import com.seinfo.seinfomationA.component.board.service.FreeBoardService;
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
@RequestMapping("/board/freeBoardList")
@RequiredArgsConstructor
@Slf4j
public class FreeBoardController {

    private final FreeBoardService freeBoardService;

    //자유게시판 첨부파일 조회용 : 토큰없이 가능 (upd 2024.04.05)
    private final TusService tusService;

    /**
     * 페이지호출 : 자유게시판 (외부:LRTP)
     */
    @GetMapping(value = "")
    public ModelAndView freeBoardBoardPage(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String searchType,
                                        @RequestParam(required = false) String freeBoardNo) {
        return freeBoardService.loadFreeBoardBoardPage(page, keyword, searchType, freeBoardNo);
    }

    /**
     * 자유게시판 목록 조회
     */
    @PostMapping(value = "/reloadFreeBoardList")
    public ResponseEntity<?> reloadFreeBoardList(@RequestBody Map<String, Object> param) {
        return freeBoardService.reloadFreeBoardList(param);
    }

    /**
     * 자유게시판 입력 : 팝업화면
     */
    @PostMapping(value = "/registFreeBoard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registFreeBoard(@RequestBody FreeBoardModel freeBoardModel, BindingResult bindingResult) {
        Map<String, Object> message = new HashMap<String, Object>();

        if(bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if(fieldError != null) {
                String errorMessage = fieldError.getDefaultMessage();
                message.put("message", errorMessage);
            }
            return ResponseEntity.badRequest().body(message);
        }
        return freeBoardService.registFreeBoard(freeBoardModel);
    }

    /**
     * 자유게시판 상세조회
     */
    @PostMapping(value = "/getFreeBoardDetail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFreeBoardDetail(@RequestBody Map<String, Object> param) {
        return freeBoardService.getFreeBoardDetail(param);
    }

    /**
     * 자유게시판 삭제
     */
    @DeleteMapping(value = "/deleteFreeBoard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFreeBoard(@RequestBody Map<String, Object> param) throws Exception{
        Map<String, Object> rtnMap = new HashMap<String, Object>();
        try {
            return freeBoardService.deleteFreeBoard(param);
        }catch (Exception e) {
            rtnMap.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(rtnMap);
        }
    }

    //자유게시판 첨부파일 조회용 : 토큰없이 가능
    @PostMapping(value = "/mfileGroupInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMfileGroupInfo(@RequestBody Map<String, Object> data) throws Exception {
        return tusService.getMfileGroupInfo(data);
    }

    //첨부팡리 목록 가져오기 - 자유게시판 첨부파일 다운용 : 토큰없이 가능
    @PostMapping(value = "/mfilePermit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mfilePermit(@RequestBody Map<String, Object> data) throws Exception {
        return tusService.getMfileDownInfo(data);
    }

    //파일 다운로드 API - 자유게시판 첨부파일 다운용 : 토큰없이 가능
    @GetMapping(path = "/filedownload")
    public ResponseEntity<?> getPrivateFileDown(HttpServletResponse response, @RequestParam String mfileDownId) throws Exception{
        Map<String, Object> param = new HashMap<>();
        param.put("mfileDownId", mfileDownId);
        return tusService.getFileDown(response, param);
    }
}


