package com.seinfo.seinfomationA.component.board.controller;



import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.seinfo.seinfomationA.component.board.Model.WorkLogModel;
import com.seinfo.seinfomationA.component.board.service.WorkLogService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class WorkLogController {

    private final WorkLogService workLogService;

    // 🔎 단일 조회 (by ID)
    @GetMapping("/api/worklog/{id}")
    @ResponseBody
    public ResponseEntity<WorkLogModel> getWorkLog(@PathVariable Long id) {
        WorkLogModel log = workLogService.getWorkLogById(id);
        return log == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(log);
    }

    // 👤 사용자 이메일로 목록 조회
    // @GetMapping("/user")
    // @ResponseBody
    // public ResponseEntity<?> getWorkLogsByEmail(@RequestParam String userEmail) {
    //     List<Map<String,Object>> logs = workLogService.getWorkLogsByUserEmail(userEmail);
    //     return ResponseEntity.ok(logs);
    // }

// ✍️ 수정
    @PutMapping("/update")
    @ResponseBody
    public ResponseEntity<Integer> updateWorkLog(@RequestBody WorkLogModel workLog) {
        int result = workLogService.updateWorkLog(workLog);
        return ResponseEntity.ok(result);
    }

    // ❌ 삭제
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Integer> deleteWorkLog(@PathVariable Long id) {
        int result = workLogService.deleteWorkLog(id);
        return ResponseEntity.ok(result);
    }

    // 📅 FullCalendar 화면 렌더링
    @GetMapping("/scheduler")
    public String showCalendarPage() {
        return "seinfo/board/scheduler";
    }


/* 단일 날짜 저장 및 파일첨부
@PostMapping(value = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
public ResponseEntity<?> insertWorkLog(@RequestBody WorkLogModel workLogModel) {

    int result = workLogService.insertWorkLog(workLogModel); 

    return ResponseEntity.ok(result);
    }
 */

// 2025 08 06 추가
@PostMapping(value = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
public ResponseEntity<?> insertWorkLog(@RequestBody WorkLogModel workLogModel) {
    
        int result = workLogService.insertWorkLog(workLogModel); // 단일 등록
        return ResponseEntity.ok(result);
}


//전체 업무일지 목록 조회 (권한 기반)
@GetMapping("/list")
@ResponseBody
public ResponseEntity<?> getAllWorkLogs() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication(); 
    String currentUserEmail = auth.getName(); // 로그인한 사용자 이메일
    boolean isAdmin = auth.getAuthorities().stream() // getAuthorities()는사용자가 관리자인지 확인,사용자의 권한 목록
        .map(GrantedAuthority::getAuthority) // 각 권한 객체에서 문자열 권한만 추출
        .anyMatch(role -> role.equals("ROLE_ADMIN")); // ROLE_ADMIN이라는 권한이 하나라도 있으면 true 반환

    List<Map<String,Object>> logs = workLogService.getAllWorkLogs(isAdmin, currentUserEmail);
    return ResponseEntity.ok(logs);
    // SecurityContextHolder 현재 로그인한 사용자 정보를 저장하는 전역 저장소
    // getContext()는 SecurityContext 객체를 반환 SecurityContext는 로그인한 사용자의 인증 정보 담고 있는 객체
    //getAuthentication()는 SecurityContext 객체에서 현재 로그인한 사용자의 인증 정보 꺼냄
    // 즉 Authentication auth = SecurityContextHolder.getContext().getAuthentication(); 이 한 줄로 로그인한 사용자의 이메일, 권한, 인증 상태 등을 가져올 수 있다.

    }
/* 2025 08 19 주석 
@PostMapping("/api/worklog/batch")
@ResponseBody
public ResponseEntity<?> saveOrUpdateWorkLogs(@RequestBody List<WorkLogModel> workLogs) {
     // 🔍 디버깅 로그 추가
    System.out.println("📥 받은 업무일지 개수: " + workLogs.size());
    for (WorkLogModel log : workLogs) {
        System.out.println("📄 업무일지: " + log);
    }
    int result = workLogService.saveOrUpdateWorkLogs(workLogs); // ✅ 서비스 호출
    return ResponseEntity.ok(result);
}
*/

/* 2025 08 19 주석
@PostMapping("/api/worklog/batch")
@ResponseBody
public ResponseEntity<?> saveOrDeleteWorkLogs(@RequestBody Map<String, List<WorkLogModel>> payload) {
    List<WorkLogModel> saveList = payload.get("saveList");
    List<WorkLogModel> deleteListRaw = payload.get("deleteList");

    List<Long> deleteList = deleteListRaw != null
        ? deleteListRaw.stream().map(WorkLogModel::getId).toList()
        : List.of();

    if (saveList != null && !saveList.isEmpty()) {
        workLogService.saveOrUpdateWorkLogs(saveList);
    }

    if (!deleteList.isEmpty()) {
        workLogService.deleteWorkLogsByIds(deleteList);
    }

    return ResponseEntity.ok().build();
}
*/

@PostMapping("/api/worklog/batch")
@ResponseBody
public ResponseEntity<?> saveOrDeleteWorkLogs(@RequestBody Map<String, List<WorkLogModel>> payload) {
    List<WorkLogModel> saveList = payload.get("saveList");
    List<WorkLogModel> deleteListRaw = payload.get("deleteList");

    // ✅ 수정된 부분: null 방지 + 유효한 ID만 필터링
    List<Long> deleteList = (deleteListRaw != null)
        ? deleteListRaw.stream()
            .map(WorkLogModel::getId)
            .filter(id -> id != null && id > 0) // null 또는 0 이하 ID 제거
            .toList()
        : List.of();

    if (saveList != null && !saveList.isEmpty()) {
        workLogService.saveOrUpdateWorkLogs(saveList);
    }

    if (!deleteList.isEmpty()) {
        workLogService.deleteWorkLogsByIds(deleteList);
    }

    return ResponseEntity.ok().build();
}

//2025 08 18 추가
@GetMapping("/api/worklog/group")
@ResponseBody
public ResponseEntity<List<WorkLogModel>> getWorkLogsByFileGrpId(@RequestParam String fileGrpId) {
    List<WorkLogModel> logs = workLogService.getWorkLogsByFileGrpId(fileGrpId);
    
    return ResponseEntity.ok(logs);
}
//승인
@PostMapping("/api/worklog/{id}/approve")
@ResponseBody
public ResponseEntity<?> approveWorkLog(@PathVariable Long id) {  // ✅ PathVariable로 변경
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_ADMIN"));

    if (!isAdmin) {
        return ResponseEntity.status(403).body("권한이 없습니다.");
    }

    String approverEmail = auth.getName(); // 로그인한 관리자의 이메일
    int result = workLogService.approveWorkLog(id, approverEmail);
    return ResponseEntity.ok(result);
}


// 반려 엔드포인트
@PostMapping("/api/worklog/{id}/reject")
@ResponseBody
public ResponseEntity<?> rejectWorkLog(
        @PathVariable Long id,                    // ✅ PathVariable로 변경
        @RequestParam String rejectionReason) {  // rejectionReason는 그대로 RequestParam 사용

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_ADMIN"));

    if (!isAdmin) {
        return ResponseEntity.status(403).body("권한이 없습니다.");
    }

    String approverEmail = auth.getName();
    int result = workLogService.rejectWorkLog(id, approverEmail, rejectionReason);
    return ResponseEntity.ok(result);
}

}
 