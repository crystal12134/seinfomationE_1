package com.seinfo.seinfomationA.component.request.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface RequestMapper {

    Map<String, Object> selectRequestUserInfo(Map<String, Object> param); // 제안서 작성자 조회 //Map 하나의 데이터 행(row)을 키-값 쌍으로 표현

    Map<String, Object> selectUserUid(Map<String, Object> param); // 작성자 유저 uid

    Map<String, Object> selectRequestRegForm(Map<String, Object> param); // 임시저장시 제안서 조회

    Map<String, Object> getLastRequestNo(); // 제안서 마지막 번호 조회

    boolean insertRequest(Map<String, Object> param); // 제안서 임시저장, 제출하기

    int deleteRequest(Map<String, Object> param); // 제안서 삭제

    Map<String, Object> selectRequestFileGrpId(Map<String, Object> param); //제안서 fileGrpId 조회

    List<Map<String, Object>> selectRequestList(Map<String, Object> param); // 제안서 이력조회 // List 여러 개의 데이터 행(row)을 리스트로 묶은 것. 각 행은 Map으로 표현됨

    int getRequestListCount(Map<String, Object> param); // 제안이력조회 게시물 수 조회

    Map<String, Object> selectRequestProcess(Map<String, Object> param);// 제안이력조회 상세 -- 프로세스

    List<Map<String, Object>> selectRequestDetail(Map<String, Object> param); //제안 이력 조회 상세 --수정요청/추가요청 이력

    Map<String, Object> selectRequestCont(Map<String, Object> param); // 의뢰상세 답변조회

    int getNextRequestSeq(Map<String, Object> param); // 마지막 번호 조회

    void insertRequestCont(Map<String, Object> param); //수정요청,추가요청 답변 삽입

}
