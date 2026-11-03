package com.seinfo.seinfomationA.component.member.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DropInfoMapper {
    Map<String, Object> selectDropUsrInfo(Map<String, Object> param); // 탈퇴할 유저 정보

    List<Map<String, Object>> selectProposalNo(Map<String, Object> param); // proposal List

    List<Map<String, Object>> selectIngProposalNo(Map<String, Object> param); // 진행중인 제안서 목록

    List<Map<String, Object>> selectMfileList(Map<String, Object> param); // lrtp_mfile_list

    //회원 자료삭제
    void deleteUsrAllTables(List<String> data);
}
