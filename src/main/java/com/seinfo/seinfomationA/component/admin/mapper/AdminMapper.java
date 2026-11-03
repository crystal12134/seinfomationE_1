package com.seinfo.seinfomationA.component.admin.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {

    List<Map<String, Object>> selectRequestListForAdmin(Map<String, Object> param);
    int getRequestListCountForAdmin();
    Map<String, Object> selectRequestDetail(Map<String, Object> param);
    List<Map<String, Object>> selectRequestContList(Map<String, Object> param);
    Map<String, Object> selectRequestContDetail(Map<String, Object> param);
    void updateRequestState(Map<String, Object> param) throws Exception;
    int getNextRequestSeq(Map<String, Object> param); // 마지막 번호 조회
    void insertRequestCont(Map<String, Object> param); //수정요청,추가요청 답변 삽입
}
