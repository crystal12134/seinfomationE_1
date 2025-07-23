package com.seinfo.seinfomationA.component.main.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface MainMapper {

    List<Map<String, Object>> selectNoticeList();

    List<Map<String, Object>> selectFaqList();
}
