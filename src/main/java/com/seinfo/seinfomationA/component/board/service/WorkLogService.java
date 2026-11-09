package com.seinfo.seinfomationA.component.board.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;


import com.seinfo.seinfomationA.component.board.Model.WorkLogModel;

public interface WorkLogService {
    //  전체 일정 목록 조회용
  List<Map<String,Object>> getAllWorkLogs(boolean isAdmin, String currentUserEmail);

    // 단일 조회
    WorkLogModel getWorkLogById(Long id);

    // 사용자 이메일 기반 목록 조회
    List<Map<String,Object>> getWorkLogsByUserEmail(String userEmail);

    // 수정
    int updateWorkLog(WorkLogModel workLog);

    // 삭제
    int deleteWorkLog(Long id);

    // 단일 일정 저장
    int insertWorkLog(WorkLogModel workLog);

  
    int saveOrUpdateWorkLogs(List<WorkLogModel> workLogs);

  // ✅ fileGrpId 기준 업무 항목 조회용 2025 08 18 추가
  List<WorkLogModel> getWorkLogsByFileGrpId(String fileGrpId);

  // 🔥 다건 삭제 기능 추가
  void deleteWorkLogsByIds(List<Long> ids);

  // 결재 승인
int approveWorkLog(Long id, String approverEmail);

// 결재 반려
int rejectWorkLog(Long id, String approverEmail, String rejectionReason);

   
}
