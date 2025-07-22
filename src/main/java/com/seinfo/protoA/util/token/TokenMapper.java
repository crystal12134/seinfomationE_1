package com.seinfo.protoA.util.token;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface TokenMapper {

    Map<String, Object> selectTokenAccess(Map<String, Object> param);

    void insertTokenBanned(Map<String, Object> param);

    void insertTokenAccess(Map<String, Object> param);
}
