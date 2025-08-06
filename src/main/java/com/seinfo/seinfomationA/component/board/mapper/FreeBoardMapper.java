package com.seinfo.seinfomationA.component.board.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface FreeBoardMapper {

    List<Map<String, Object>> selectFreeBoardList(Map<String,Object> param); //자유게시판 게시물 조회

    int getFreeBoardListCount(Map<String, Object> param); //자유게시판 게시물 수 조회

    void insertFreeBoard(Map<String, Object> param); // 자유게시판 게시물 삽입

    void updateFreeBoard(Map<String, Object> param); // 자유게시판 게시물 수정

    Map<String, Object> selectFreeBoardDetail(Map<String, Object> param); // 자유게시판 상세보기

    void updateInquireCnt(Map<String, Object> param); // 게시물 조회수 업데이트

    Map<String, Object> selectFreeBoardFileGrpId(Map<String, Object> param); // 자유게시판 게시물 fileGrpId 조회

    int deleteFreeBoard(Map<String, Object> param); //게시물 삭제
}
