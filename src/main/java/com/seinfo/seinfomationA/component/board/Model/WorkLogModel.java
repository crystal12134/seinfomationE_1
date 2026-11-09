package com.seinfo.seinfomationA.component.board.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
@Data
public class WorkLogModel {
    
    private Long id;                    // 업무일지 고유번호
    private String userEmail;           // 작성자 이메일
    private String title;              // 제목
    private String content;            // 상세 내용
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime  workDate;        // 단일 날짜 저장용 업무 수행일
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;   // 작성 시간
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;   // 수정 시간
    
     private String userNm;  // user_nm 컬럼 매핑용 필드 추가
    private Double hours;        // 업무 소요 시간
    private String category;     // 업무 분류
    private String fileGrpId;  // 2025 07 30 추가 첨부파일 그룹 ID (공통파일 연동용)
    private boolean deleted; // 삭제 대상 여부 2025 08 19 추가

        // ✅ DB 매핑 추가
    private String approvalStatus;   // PENDING, APPROVED, REJECTED
    private String approvedBy;       // 결재자 이메일
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt; // 결재일자
    private String rejectionReason;  // 반려 사유
}
