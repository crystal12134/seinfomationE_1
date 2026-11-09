package com.seinfo.seinfomationA.component.board.controller;

import com.seinfo.seinfomationA.util.EncriptUtil;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.component.board.Model.FreeBoardModel;
import com.seinfo.seinfomationA.component.board.service.FreeBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/board/freeBoardList")
@RequiredArgsConstructor
@Slf4j
public class FreeBoardController {

    private final FreeBoardService freeBoardService;
    private final ResponseEntityUtil responseEntityUtil;

    /**
     * 페이지호출 : 자유게시판 (외부:LRTP)
     */
    @GetMapping(value = "")
    public ModelAndView freeBoardBoardPage(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String searchType,
                                        @RequestParam(required = false) String freeboardNo) {
        return freeBoardService.loadFreeBoardBoardPage(page, keyword, searchType, freeboardNo);
    }

    @PostMapping(value = "/reloadFreeBoardList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reloadFreeBoardList(@RequestBody Map<String, Object> param) {
        return freeBoardService.reloadFreeBoardList(param);
    }
    

    /**
     * 자유게시판 상세조회 화면
     */
    @GetMapping(value = "/freeBoardDetail")
    public ModelAndView getFreeBoardDetail(@RequestParam(required = false) String paramData) {
        Map<String, Object> param = fnDecParam(paramData);

        return freeBoardService.getFreeBoardDetail(param);
    }

    /**
     * 자유게시판 상세 재조회
     */
    @PostMapping(value = "/reloadFreeBoardDetail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reloadFreeBoardDetail(@RequestBody Map<String, Object> param) {
        return freeBoardService.reloadFreeBoardDetail(param);
    }

     /**
     * 자유게시판 대댓글 조회
     */
    @PostMapping(value = "/addCommentList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addCommentList(@RequestBody Map<String, Object> param) {
        return freeBoardService.getFreeBoardFarentComment(param);
    }

    /**
     * 자유게시판 댓글 삭제 (대댓글 포함)
     */
    @PostMapping("/deleteComment")
    public ResponseEntity<?> deleteComment(@RequestBody Map<String, Object> param) {
        try {
            return freeBoardService.deleteComment(param);
        }catch(Exception e){
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
    }

    /**
     * 자유게시판 대댓글 삭제
     */
    @PostMapping("/deleteParentComment")
    public ResponseEntity<?> deleteParentComment(@RequestBody Map<String, Object> param) {
        try {
            return freeBoardService.deleteParentComment(param);
        }catch(Exception e){
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
    }

    /**
     * 자유게시판 게시글 삭제
     */
    @PostMapping("/deletePost")
    public ResponseEntity<?> deleteFreeboardPost(@RequestBody Map<String, Object> param) throws Exception {
        try {
            return freeBoardService.deletePostWithComments(param);
        }catch(Exception e){
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
    }


     /**
     * 자유게시판 댓글 수정
     */
    @PostMapping("/updateComment")
    public ResponseEntity<?> updateComment(@RequestBody Map<String, Object> param) {
        try {
            freeBoardService.updateComment(param); // void 메서드 호출
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("요청 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Collections.singletonMap("success", false));
        }
    }

    /**
     * 자유게시판 대댓글 수정
     */
    @PostMapping("/updateParentComment")
    public ResponseEntity<?> updateParentComment(@RequestBody Map<String, Object> param) {
        try {
            freeBoardService.updateFarentComment(param); // void 메서드 호출
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("요청 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Collections.singletonMap("success", false));
        }
    }

    /**
     * 자유게시판 상세 글 수정
     */
   @PutMapping(value = "/updateFreeBoard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateFreeBoard(@RequestBody FreeBoardModel freeBoardModel, BindingResult bindingResult) {
        Map<String, Object> message = new HashMap<>();

        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                String errorMessage = fieldError.getDefaultMessage();
                message.put("message", errorMessage);
            }
            return ResponseEntity.badRequest().body(message);
        }

        return freeBoardService.updateFreeBoard(freeBoardModel);
    }



    /**
     * 자유게시판 상세 댓글 등록
     */
    @PostMapping(value = "/registFreeBoardComment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registFreeBoardComment(@RequestBody Map<String, Object> param) throws Exception {
        return freeBoardService.registFreeBoardComment(param);
    }

    /**
     * 자유게시판 상세페이지 게시글 추천 비추천
     */
    @PostMapping(value = "/likePost", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> likePost(@RequestBody Map<String, Object> param) {
        try {
            return freeBoardService.likePost(param);
        }catch(Exception e){
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

     /**
     * 자유게시판 상세페이지 댓글 추천 비추천
     */
    @PostMapping(value = "/likeComment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> likeComment(@RequestBody Map<String, Object> param) {
        try {
            return freeBoardService.likeComment(param);
        }catch(Exception e){
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
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
     * 복호화
     */
    private Map<String, Object> fnDecParam(String paramData) {
        Map<String, Object> param = new HashMap<>();
        if (paramData != null && !paramData.isBlank()) {
            try {
                String decParamData = EncriptUtil.decryptAes256(paramData);

                if (!decParamData.isBlank()) {
                    String[] params = decParamData.split("&");
                    param = Arrays.stream(params)
                            .map(p -> p.split("=", 2))
                            .filter(p -> p.length == 2)
                            .collect(Collectors.toMap(p -> p[0], p -> p[1]));
                }
            } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
                log.warn("파라미터 복호화 실패 or 파싱 실패: {}", e.getMessage());
            }
        }
        return param;
    }
}


