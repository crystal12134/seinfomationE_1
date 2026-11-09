package com.seinfo.seinfomationA.component.board.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.seinfo.seinfomationA.component.board.Model.WorkLogModel;

@Mapper
public interface WorkLogMapper {
    
     // 저장
    int insertWorkLog(WorkLogModel workLog);

    // 단일 조회
    WorkLogModel selectWorkLogById(Long id);

    // 사용자 이메일 기반 목록 조회
    List<Map<String,Object>> selectWorkLogsByUserEmail(String userEmail);

    // 수정
    int updateWorkLog(WorkLogModel workLog);

    // 삭제
    int deleteWorkLog(Long id);
    
    //전체 일정 목록 조회 자바에서 권한 체크
    //List<Map<String,Object>> selectAllWorkLogs();  

    //전체 일정 목록 조회 sql에서 권한 체크
    List<Map<String,Object>> selectAllWorkLogs(@Param("isAdmin") boolean isAdmin,
                                           @Param("currentUserEmail") String currentUserEmail);
     // 다중데이터저장 시
     int insertWorkLogs(@Param("list") List<WorkLogModel> workLogs);

     //2025 08 18 추가
     // ✅ fileGrpId 기준 업무 항목 조회
List<WorkLogModel> selectWorkLogsByFileGrpId(@Param("fileGrpId") String fileGrpId);

//  ID 목록 기반 삭제 202 5 08 19 추가
void deleteByIds(@Param("ids") List<Long> ids);

// 결재 승인
int approveWorkLog(
    @Param("id") Long id,
    @Param("approverEmail") String approverEmail,
    @Param("approvedAt") String approvedAt
);

// 결재 반려
int rejectWorkLog(
    @Param("id") Long id,
    @Param("approverEmail") String approverEmail,
    @Param("approvedAt") String approvedAt,
    @Param("rejectionReason") String rejectionReason
);

}
