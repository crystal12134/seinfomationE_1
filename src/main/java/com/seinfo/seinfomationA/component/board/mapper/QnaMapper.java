package com.seinfo.seinfomationA.component.board.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface QnaMapper {

    List<Map<String, Object>> selectQnaList(Map<String, Object> param); //QnA 목록조회 (1:1 문의)

    int getQnaListCount(Map<String,Object> param); //QnA 목록 총건수(1:1 문의)

    void insertQna(Map<String, Object> param); //QnA 질문 등록

    void updateQna(Map<String, Object> param); //QnA 질문 수정

    void updateQnaDel(Map<String, Object> param); //QnA 질문 삭제

    void updateAnswer(Map<String, Object> param); //QnA 답변 작성

    void updateAnswerDel(Map<String, Object> param); //QnA 답변 삭제

    Map<String, Object> selectQnaDetail(Map<String, Object> param);


}
