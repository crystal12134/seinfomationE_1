package com.seinfo.protoA.util.api;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AipMapper {

    List<Map<String, Object>> getCodeList(Map<String, Object> param);
}
