package com.seinfo.seinfomationA.util.api;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApiMapper {

    List<Map<String, Object>> getCodeList(Map<String, Object> param);

    List<Map<String, Object>> getAdminMailInfo();

    Map<String, Object> getUserEmailInfo(Map<String, Object> param);
}
