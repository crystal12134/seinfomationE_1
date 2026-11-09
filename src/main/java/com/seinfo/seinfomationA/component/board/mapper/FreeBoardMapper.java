package com.seinfo.seinfomationA.component.board.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.seinfo.seinfomationA.component.board.Model.FreeBoardModel;

import java.util.List;
import java.util.Map;

@Mapper
public interface FreeBoardMapper {

    List<Map<String, Object>> selectFreeBoardList(Map<String,Object> param); //자유게시판 게시물 조회

    int getFreeBoardListCount(Map<String, Object> param); //자유게시판 게시물 수 조회

    void insertFreeBoard(Map<String, Object> param); // 자유게시판 게시물 삽입

    int updateFreeBoard(Map<String, Object> param); // 자유게시판 게시물 수정

    Map<String, Object> selectFreeBoardDetail(Map<String, Object> param); // 자유게시판 상세보기

    List<Map<String, Object>> selectFreeBoardComment(Map<String, Object> param); // 자유게시판 상세 댓글 조회

    List<Map<String, Object>> selectFreeBoardAddComment(Map<String, Object> param); // 자유게시판 상세 대댓글 조회

    int deleteFreeBoardCommentWithChild(Map<String, Object> param); // 자유게시판 상세보기 댓글 삭제 (대댓글 포함)

    int deleteChildComment(Map<String, Object> param); // 자유게시판 상세보기 대댓글 삭제

    void updateInquireCnt(Map<String, Object> param); // 게시물 조회수 업데이트

    int deleteFreeBoardCommentsByPost(Map<String,Object> param);

    //Map<String, Object> selectFreeBoardFileGrpId(Map<String, Object> param); // 자유게시판 게시물 fileGrpId 조회

    int deleteFreeBoardPost(Map<String, Object> param); //자유게시판 게시글 삭제

    void updateFreeBoardDetailLike(Map<String, Object> param); // 자유게시판 추천 비추천 

    void increaseCommentLike(Map<String, Object> param); // 원댓글 추천 비추천
    
    int insertFreeBoardComment(Map<String, Object> param); // 원댓글 등록 

    void updateComment(Map<String, Object> param); // 원댓글 수정  

    void updateFarentComment(Map<String, Object> param); // 대댓글 수정

    Map<String, Object> selectFreeBoardLikeByUser(Map<String, Object> param); // 게시글 기존 유저 추천 여부

    void insertFreeBoardLike(Map<String, Object> param); // 게시글 최초 추천/비추천

    void updateFreeBoardLike(Map<String, Object> param); // 게시글 추천 상태 변경

    void deleteFreeBoardLike(Map<String, Object> param); // 게시글 추천/비추천 취소

}
