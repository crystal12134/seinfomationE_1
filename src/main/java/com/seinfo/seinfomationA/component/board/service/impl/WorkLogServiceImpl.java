package com.seinfo.seinfomationA.component.board.service.impl;

import com.seinfo.seinfomationA.util.security.ResourceAccess;
import com.seinfo.seinfomationA.util.security.SafeContent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.seinfo.seinfomationA.component.board.Model.WorkLogModel;
import com.seinfo.seinfomationA.component.board.mapper.WorkLogMapper;
import com.seinfo.seinfomationA.component.board.service.WorkLogService;

import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@org.springframework.transaction.annotation.Transactional
@Service
@RequiredArgsConstructor
public class WorkLogServiceImpl  implements WorkLogService{
    private final ResourceAccess resourceAccess;

    
      private final WorkLogMapper workLogMapper;

    @Override // 부모 클래스 이름이나 매개변수가 변경될 경우 @Override가 달려 있으면 오류를 바로 알수있다.가독성,정확하게 같은 메서드가 있는지 검사 등등 그냥 붙여주자.
    public WorkLogModel getWorkLogById(Long id) {
        resourceAccess.work(id);

        return workLogMapper.selectWorkLogById(id);
    }

    @Override
    public List<Map<String,Object>> getWorkLogsByUserEmail(String userEmail) {
        resourceAccess.requireStaff();
        if(!resourceAccess.admin()) userEmail=resourceAccess.email();

        return workLogMapper.selectWorkLogsByUserEmail(userEmail);
    }

    @Override
    public int updateWorkLog(WorkLogModel workLog) {
        resourceAccess.work(workLog.getId());
        resourceAccess.attachedGroup(workLog.getFileGrpId());
        workLog.setTitle(SafeContent.text(workLog.getTitle()));
        workLog.setContent(SafeContent.text(workLog.getContent()));

        return workLogMapper.updateWorkLog(workLog);
    }

    @Override
    public int deleteWorkLog(Long id) {
        resourceAccess.work(id);

        return workLogMapper.deleteWorkLog(id);
    }

    // 📝 일지 저장
@Override
public int insertWorkLog(WorkLogModel workLog) {
        resourceAccess.requireStaff();
        workLog.setUserEmail(resourceAccess.email());
        workLog.setDeleted(false);
        resourceAccess.attachedGroup(workLog.getFileGrpId());
        workLog.setTitle(SafeContent.text(workLog.getTitle()));
        workLog.setContent(SafeContent.text(workLog.getContent()));



    try {
        int result = workLogMapper.insertWorkLog(workLog);
        System.out.println("✅ DB 저장 결과: " + result);

        if (result == 0) {
            System.out.println("⚠️ DB에 반영된 행이 없습니다. 조건을 다시 확인하세요.");
        }

        return result;
    } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
        System.out.println("❌ DB 저장 중 예외 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        e.printStackTrace(); // 콘솔에 전체 스택 출력
        return 0; // 여기 나중에 다시 잡기
    }
}

    //권한에따른 조회
    @Override
    public List<Map<String,Object>> getAllWorkLogs(boolean isAdmin, String currentUserEmail) {
        resourceAccess.requireStaff();
        isAdmin=resourceAccess.admin();
        currentUserEmail=resourceAccess.email();

        return workLogMapper.selectAllWorkLogs(isAdmin, currentUserEmail); // 권한은 SQL에서 처리
    }

@Override
public int saveOrUpdateWorkLogs(List<WorkLogModel> workLogs) {
        resourceAccess.requireStaff();
        for(WorkLogModel item:workLogs) if(item.getId()!=null) resourceAccess.work(item.getId());

    int count = 0;
    for (WorkLogModel log : workLogs) {
        if (log.getId() != null) {
            // 기존 데이터 수정
            count += updateWorkLog(log);
        } else {
            // 새 데이터 등록
            count += insertWorkLog(log);
        }
    }
    return count;
}

    //2025 08 18 추가
    @Override
    public List<WorkLogModel> getWorkLogsByFileGrpId(String fileGrpId) {
        resourceAccess.requireStaff();
        resourceAccess.fileGroup(fileGrpId,false,false);

    return workLogMapper.selectWorkLogsByFileGrpId(fileGrpId);
}

@Override
public void deleteWorkLogsByIds(List<Long> ids) {
        for(Long id:ids) resourceAccess.work(id);

    workLogMapper.deleteByIds(ids);
}

   @Override
    public int approveWorkLog(Long id, String approverEmail) {
        resourceAccess.requireAdmin();
        approverEmail=resourceAccess.email();

        // 수정됨: MySQL DATETIME 형식으로 변환
        String approvedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // 수정됨
        return workLogMapper.approveWorkLog(id, approverEmail, approvedAt);
    }

    @Override
    public int rejectWorkLog(Long id, String approverEmail, String rejectionReason) {
        resourceAccess.requireAdmin();
        approverEmail=resourceAccess.email();

        // 수정됨: MySQL DATETIME 형식으로 변환
        String approvedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // 수정됨
        return workLogMapper.rejectWorkLog(id, approverEmail, approvedAt, rejectionReason);
    }

}