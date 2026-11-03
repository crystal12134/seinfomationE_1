package com.seinfo.seinfomationA.component.board.service.impl;

import com.seinfo.seinfomationA.util.security.ResourceAccess;
import com.seinfo.seinfomationA.util.security.SafeContent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seinfo.seinfomationA.component.board.Model.FreeBoardModel;
import com.seinfo.seinfomationA.component.board.mapper.FreeBoardMapper;
import com.seinfo.seinfomationA.component.board.service.FreeBoardService;
import com.seinfo.seinfomationA.component.file.mapper.TusMapper;
import com.seinfo.seinfomationA.component.file.service.TusService;
import com.seinfo.seinfomationA.util.ComUtil;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.CoreWebApiResponseModel;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.security.AuthUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;

import jakarta.mail.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreeBoardServiceImpl implements FreeBoardService {
    private final ResourceAccess resourceAccess;


    private final TokenUtils tokenUtils;
    private final ComUtil comUtil;
    private final FreeBoardMapper freeBoardMapper;
    private final TusMapper tusMapper;
    private final TusService tusService;
    private final AuthUtil authUtil;
    private final HttpSession httpSession;
    private final HttpServletRequest request;
    private final ResponseEntityUtil responseEntityUtil;

    /**
     * 페이지 호출 : 자유게시판 (외부:LRTP) / (내부:RTP)
     */
    @SuppressWarnings("unchecked")
    public ModelAndView loadFreeBoardBoardPage(int page, String keyword, String searchType, String freeboardNo) {
        boolean adminYn = false;

        Map<String, Object> param = new HashMap<>();
        ModelAndView mv = new ModelAndView();
        String userType = Objects.toString(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_TYPE), "USER");
        mv.addObject("userType", userType);

        // 내부 : 토큰 유효성 체크 + 권한 조회
        if (authUtil.isRoleInTokenAndAuth("ADMIN")) { //최초 페이지 호출시, 1번만.
            adminYn = true;
        }
        mv.setViewName("seinfo/board/freeBoardList");

        //전체 레코드 수 조회
        param.put("keyword", keyword);
        param.put("searchType", searchType);
        int totalRecords = freeBoardMapper.getFreeBoardListCount(param);

        //페이지 정보 계산
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); //최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); //현재 페이지 범위 제한
        int startRow = (cPage - 1) * pageSize;

        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> freeBoardList = freeBoardMapper.selectFreeBoardList(param);

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalRecords", totalRecords);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage < 1);
        pageResult.put("hNext", cPage < totalPages);

        mv.addObject("freeBoardList", freeBoardList);
        mv.addObject("pageResult", pageResult);
        mv.addObject("page", page);
        mv.addObject("keyword", keyword);
        mv.addObject("searchType", searchType);
        mv.addObject("freeboardNo", freeboardNo);
        mv.addObject("adminYn", adminYn);

        return mv;

    }

    /**
     * 자유게시판 목록 재조회
     */
    @Override
    public ResponseEntity<?> reloadFreeBoardList(Map<String, Object> param){
        Map<String, Object> resultMap = new HashMap<>();

        // 전체 레코드 수 조회
        int totalRecords = freeBoardMapper.getFreeBoardListCount(param);

        // 페이지 정보 계산
        int page = Integer.parseInt(param.get("page").toString());
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); // 최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); // 현재 페이지 범위 제한
        int startRow = (cPage -1) * pageSize;

        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> freeBoardList = freeBoardMapper.selectFreeBoardList(param);

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalRecords", totalRecords);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage);
        pageResult.put("hNext", cPage < totalPages);

        resultMap.put("freeBoardList", freeBoardList);
        resultMap.put("pageResult", pageResult);

        return ResponseEntity.ok(resultMap);
    }

    /**
     * 자유게시판 추천 비추천 
     */
    @Override
    @Transactional
    public ResponseEntity<?> likePost(Map<String, Object> param) throws Exception {
        resourceAccess.email();

        String postNo = String.valueOf(param.get("freeboardNo"));
        String likeSort = String.valueOf(param.get("likeSort")); // "Y" or "N"
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 기존 기록 확인
        Map<String, Object> checkParam = new HashMap<>();
        checkParam.put("freeboardNo", postNo);
        checkParam.put("userId", userId);

        Map<String, Object> existing = freeBoardMapper.selectFreeBoardLikeByUser(checkParam);

        if (existing == null) {
            // 최초 추천/비추천
            param.put("userId", userId);
            freeBoardMapper.insertFreeBoardLike(param);
            freeBoardMapper.updateFreeBoardDetailLike(param);
        } else {
            String existingSort = String.valueOf(existing.get("like_sort"));

            if (existingSort.equals(likeSort)) {
                // 같은 버튼 두 번 누름 → 취소
                freeBoardMapper.deleteFreeBoardLike(checkParam);
                param.put("cancel", true);
                freeBoardMapper.updateFreeBoardDetailLike(param);
            } else {
                // 다른 버튼 누름 → 상태 변경
                param.put("userId", userId);
                freeBoardMapper.updateFreeBoardLike(param); // UPDATE like_sort
                param.put("change", true);
                freeBoardMapper.updateFreeBoardDetailLike(param);
            }
        }

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "success");
    }


    /**
     * 자유게시판 원댓글 대댓글 추천 비추천 
     */
    @Override
    @Transactional
    public ResponseEntity<?> likeComment(Map<String, Object> param) throws Exception {
        resourceAccess.email();


        String commentNo = String.valueOf(param.get("commentNo"));
        String likeSort = String.valueOf(param.get("likeSort"));
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        String commentCase;

        // 기존 기록 확인
        Map<String, Object> checkParam = new HashMap<>();
        checkParam.put("commentNo", commentNo);
        checkParam.put("userId", userId);

         Map<String, Object> existing = freeBoardMapper.selectFreeBoardLikeByUser(checkParam);
        if (existing == null) {
            // 최초 추천/비추천
            param.put("userId", userId);
            freeBoardMapper.insertFreeBoardLike(param);
            freeBoardMapper.increaseCommentLike(param);
            commentCase = "A";
        } else {
            String existingSort = String.valueOf(existing.get("like_sort"));

            if (existingSort.equals(likeSort)) {
                // 같은 버튼 두 번 누름 → 취소
                freeBoardMapper.deleteFreeBoardLike(checkParam);
                param.put("cancel", true);
                freeBoardMapper.increaseCommentLike(param);
                commentCase = "B";
            } else {
                // 다른 버튼 누름 → 상태 변경
                param.put("userId", userId);
                freeBoardMapper.updateFreeBoardLike(param); // UPDATE like_sort
                param.put("change", true);
                freeBoardMapper.increaseCommentLike(param);
                commentCase = "C";
            }
        }
        
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", commentCase);
    }

    /**
     * 자유게시판 글쓰기 : 팝업화면
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public ResponseEntity<?> registFreeBoard(FreeBoardModel freeBoardModel) {
        resourceAccess.email();
        freeBoardModel.setFreeBoardTitle(SafeContent.text(freeBoardModel.getFreeBoardTitle()));
        freeBoardModel.setFreeBoardContent(SafeContent.html(freeBoardModel.getFreeBoardContent()));

        Map<String, Object> param = new ObjectMapper().convertValue(freeBoardModel, Map.class);
        Map<String, Object> rtnResult = new HashMap<String, Object>();
        rtnResult.put("message", "");


        //저장전 체크
        if(Objects.isNull(param.get("freeBoardTitle")) || "".equals(param.get("freeBoardTitle"))) {
            rtnResult.put("message", "제목을 입력하세요.");
        } else if (Objects.isNull(param.get("freeBoardContent")) || "".equals(param.get("freeBoardContent"))) {
            rtnResult.put("message", "내용을 입력하세요.");
        }

        if(!rtnResult.get("message").toString().isEmpty()) {
            return ResponseEntity.badRequest().body(rtnResult);
        }

        //입력값 설정
        param.put("userId", tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID));

        //입력쿼리
        try{
            if(Objects.equals("Y", param.get("modifyYn"))) {
                updateOwnedPost(freeBoardModel); // 모든 수정 경로에 동일한 권한 검사
            } else {
                freeBoardMapper.insertFreeBoard(param); //등록
            }
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("registFreeBoard : {}", e.getMessage());
            rtnResult.put("message", "서버 내 오류가 발생하였습니다.");
            return ResponseEntity.internalServerError().body(rtnResult);
        }

        rtnResult.put("success", "success");
        return ResponseEntity.ok(rtnResult);
    }

     /**
     * 자유게시판 게시글 수정 
     */
    @Override
    @Transactional
    public ResponseEntity<?> updateFreeBoard(FreeBoardModel freeBoardModel) {
        updateOwnedPost(freeBoardModel);
        return ResponseEntity.ok().body(Map.of("message", "수정되었습니다."));
    }

    private void updateOwnedPost(FreeBoardModel model) {
        resourceAccess.post(model.getFreeBoardNo());
        model.setFreeBoardTitle(SafeContent.text(model.getFreeBoardTitle()));
        model.setFreeBoardContent(SafeContent.html(model.getFreeBoardContent()));
        Map<String,Object> param = new ObjectMapper().convertValue(model, Map.class);
        param.put("actorEmail", resourceAccess.email());
        param.put("actorAdmin", resourceAccess.admin());
        if (freeBoardMapper.updateFreeBoard(param) != 1) {
            throw new org.springframework.security.access.AccessDeniedException("게시글 수정 권한이 없습니다.");
        }
    }

    /**
     * 자유게시판 상세조회 
     */
    @Override
    public ModelAndView getFreeBoardDetail(Map<String, Object> param) {

        Map<String, Object> resultMap = freeBoardMapper.selectFreeBoardDetail(param);
        List<Map<String, Object>> commentList = freeBoardMapper.selectFreeBoardComment(param);

        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/board/freeBoardDetail");

        if(!Objects.equals("Y", param.get("modifyYn"))) { //수정모드 일 때
            int inquireCnt = Integer.parseInt(resultMap.get("inquireCnt").toString()) + 1;
            param.put("inquireCnt", inquireCnt);
            resultMap.replace("inquireCnt", inquireCnt);
            freeBoardMapper.updateInquireCnt(param);
        }

        mv.addObject("freeBoardDetail", resultMap);
        mv.addObject("commentList", commentList); // 전체 댓글
    
        return mv;
    }

    /**
     * 자유게시판 상세 목록 재조회  
     */
    @Override
    public ResponseEntity<?> reloadFreeBoardDetail(Map<String, Object> param) {

        Map<String, Object> freeBoardDetail = freeBoardMapper.selectFreeBoardDetail(param);
        List<Map<String, Object>> commentList = freeBoardMapper.selectFreeBoardComment(param);

        if(!Objects.equals("Y", param.get("modifyYn"))) { //수정모드 일 때
            int inquireCnt = Integer.parseInt(freeBoardDetail.get("inquireCnt").toString()) + 1;
            param.put("inquireCnt", inquireCnt);
            freeBoardDetail.replace("inquireCnt", inquireCnt);
            //freeBoardMapper.updateInquireCnt(param);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("freeBoardDetail", freeBoardDetail);
        resultMap.put("commentList", commentList);

        return ResponseEntity.ok(resultMap);
    }

     /**
     * 자유게시판 상세페이지 댓글 저장
     */

    @Override
    @Transactional
    public ResponseEntity<?> registFreeBoardComment(Map<String, Object> param) throws Exception {
        resourceAccess.email();
        param.put("commentContent", SafeContent.text((String)param.get("commentContent")));

        Object currentUser = tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID);
        if (currentUser == null || currentUser.toString().isBlank()) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        param.put("userId", currentUser);
        try {
            // 댓글 저장 쿼리 호출
            freeBoardMapper.insertFreeBoardComment(param);

            // 성공 응답
            return ResponseEntity.ok(Map.of("message", "댓글이 등록되었습니다."));
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("registFreeBoardComment : {}", e.getMessage());
            throw new RuntimeException("댓글 등록 중 오류가 발생했습니다.");
        }
    }

    /**
     * 자유게시판 상세페이지 댓글 데이터
     */
    @Override
    public List<Map<String, Object>> getFreeBoardComment(Map<String, Object> param) {
        return freeBoardMapper.selectFreeBoardComment(param);
    }

    /**
     * 자유게시판 상세페이지 대댓글 데이터
     */
    @Override
    public ResponseEntity<?> getFreeBoardFarentComment(Map<String, Object> param) {
        List<Map<String, Object>> addcommentList = freeBoardMapper.selectFreeBoardAddComment(param);
        return ResponseEntity.ok(Map.of("addCommentList",addcommentList ));
    }

    /**
     * 자유게시판 상세페이지 댓글 삭제 (대댓글 포함)
     */
    @Override
    @Transactional
    public ResponseEntity<?>  deleteComment(Map<String, Object> param) throws Exception {
        resourceAccess.comment(param.get("commentNo"));

        freeBoardMapper.deleteFreeBoardCommentWithChild(param);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "success");
    }
    
     /**
     * 자유게시판 상세페이지 대댓글 삭제
     */
    @Override
    @Transactional
    public ResponseEntity<?> deleteParentComment(Map<String, Object> param) throws Exception {
        resourceAccess.comment(param.get("commentNo"));

        freeBoardMapper.deleteChildComment(param);
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "success");
    }
    
     /**
     * 자유게시판 게시글 삭제 (댓글도 함께 삭제)
     */
     @Override
     @Transactional
     public ResponseEntity<?> deletePostWithComments(Map<String, Object> param) throws Exception {
        resourceAccess.post(param.get("freeboardNo"));

         // 검증한 게시글 번호에 속한 댓글만 삭제합니다. 요청의 commentNo는 사용하지 않습니다.
         Map<String,Object> verified = new HashMap<>();
         verified.put("freeboardNo", ResourceAccess.id(param.get("freeboardNo")));
         verified.put("actorEmail", resourceAccess.email());
         verified.put("actorAdmin", resourceAccess.admin());
         freeBoardMapper.deleteFreeBoardCommentsByPost(verified);
         if (freeBoardMapper.deleteFreeBoardPost(verified) != 1) {
             throw new org.springframework.security.access.AccessDeniedException("게시글 삭제 권한이 없습니다.");
         }
         return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "success");
     }

     /**
     * 자유게시판 댓글 수정 
     */
     @Override
     @Transactional
     public ResponseEntity<?> updateComment(Map<String, Object> param) throws Exception {
        resourceAccess.comment(param.get("commentNo"));
        param.put("commentContent", SafeContent.text((String)param.get("commentContent")));

         freeBoardMapper.updateComment(param);
         return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "success");
     }

     /**
     * 자유게시판 대댓글 수정 
     */
     @Override
     @Transactional
     public ResponseEntity<?>  updateFarentComment(Map<String, Object> param) throws Exception {
        resourceAccess.comment(param.get("commentNo"));
        param.put("commentContent", SafeContent.text((String)param.get("commentContent")));

         freeBoardMapper.updateFarentComment(param);
         return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "success");
     }

}
