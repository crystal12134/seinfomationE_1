package com.seinfo.seinfomationA.component.board.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface FaqMapper {

    List<Map<String, Object>> selectFaqList(Map<String, Object> param); // faq 게시물 조회

    int getFqaListCount(Map<String, Object> param); // faq 게시물 수 조회

    void insertFqa(Map<String, Object> param); //faq 게시물 삽입

    void updateInquireCnt(Map<String, Object> param); // 조회수 증가

    int selectFaqRnum(Map<String, Object> param); // faqNo별 위치조회
}
