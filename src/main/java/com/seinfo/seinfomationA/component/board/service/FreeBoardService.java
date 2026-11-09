package com.seinfo.seinfomationA.component.board.service;

import com.seinfo.seinfomationA.component.board.Model.FreeBoardModel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

public interface FreeBoardService {

    ModelAndView loadFreeBoardBoardPage(int page, String keyword, String searchType, String freeBoardNo);

    ResponseEntity<?> reloadFreeBoardList(Map<String, Object> param);   // 자유게시판 목록 조회

    ResponseEntity<?> registFreeBoardComment(Map<String, Object> param) throws Exception; // 자유게시판 상세페이지 댓글 조회

    ResponseEntity<?> likePost(Map<String, Object> param) throws Exception;   // 추천 기능
    
    ResponseEntity<?> likeComment(Map<String, Object> param) throws Exception;   // 원댓글 추천 기능

    ResponseEntity<?> registFreeBoard(FreeBoardModel freeBoardModel);

    ModelAndView getFreeBoardDetail(Map<String, Object> param);

    List<Map<String, Object>> getFreeBoardComment(Map<String, Object> param); // 상세페이지 댓글 데이터 

    ResponseEntity<?> getFreeBoardFarentComment(Map<String, Object> param); // 상세페이지 대댓글 데이터 

    ResponseEntity<?>  deleteComment(Map<String, Object> param) throws Exception; // 상세페이지 댓글 삭제 (대댓글 포함)

    ResponseEntity<?>  deleteParentComment(Map<String, Object> param) throws Exception; // 상세페이지 대댓글 삭제

    ResponseEntity<?> reloadFreeBoardDetail(Map<String, Object> param);

    ResponseEntity<?>  deletePostWithComments(Map<String, Object> param) throws Exception; // 상세페이지 글 삭제

    ResponseEntity<?> updateFreeBoard(FreeBoardModel freeBoardModel); // 상세페이지 글 수정

    ResponseEntity<?> updateComment(Map<String, Object> param) throws Exception; // 상세페이지 댓글 수정

    ResponseEntity<?> updateFarentComment(Map<String, Object> param) throws Exception; // 상세페이지 대댓글 수정

}
