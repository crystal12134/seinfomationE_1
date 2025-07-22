package com.seinfo.protoA.component.board.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeMapper {

    List<Map<String, Object>> selectNoticeList(Map<String,Object> param); //공지사항 게시물 조회

    int getNoticeListCount(Map<String, Object> param); //공지사항 게시물 수 조회

    void insertNotice(Map<String, Object> param); // 공지사항 게시물 삽입

    void updateNotice(Map<String, Object> param); // 공지사항 게시물 수정

    Map<String, Object> selectNoticeDetail(Map<String, Object> param); // 공지사항 상세보기

    void updateInquireCnt(Map<String, Object> param); // 게시물 조회수 업데이트

    Map<String, Object> selectNoticeFileGrpId(Map<String, Object> param); // 공지사항 게시물 fileGrpId 조회

    int deleteNotice(Map<String, Object> param); //게시물 삭제
}
