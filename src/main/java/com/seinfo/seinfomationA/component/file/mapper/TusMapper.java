package com.seinfo.seinfomationA.component.file.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface TusMapper {

    int selectMfileListCnt(Map<String, Object> param) throws Exception;
    int insertMfileList(Map<String, Object> param) throws Exception;
    int updateMfileList(Map<String, Object> param) throws Exception;
    List<Map<String, Object>> selectMfileList(Map<String, Object> param) throws Exception;
    List<Map<String, Object>> selectMfileListClient(Map<String, Object> param) throws Exception;
    Map<String, Object> selectMfileInfo(Map<String, Object> param) throws Exception;
    Map<String, Object> selectMfileInfoByFileGroupId(Map<String, String> param) throws Exception;
    int insertMfileGroup(Map<String, Object> param) throws Exception;
    int selectMfileGroupCnt(Map<String, Object> param) throws Exception;
    Map<String, Object> selectMfileGroup(Map<String, Object> param) throws Exception;
    int insertMfileDown(Map<String, Object> param) throws Exception;
    Map<String, Object> selectMfileDown(Map<String, Object> param) throws Exception;
    List<Map<String, Object>> selectMfileListPath(Map<String, Object> param) throws Exception;
    int updateMfileDown(Map<String, Object> param) throws Exception;
    void deleteMfileList(Map<String, Object> param) throws Exception;
    void deleteMfileGroup(Map<String, Object> param) throws Exception;
    Map<String, Object> selectMfilePath(Map<String, Object> param) throws Exception;
}
