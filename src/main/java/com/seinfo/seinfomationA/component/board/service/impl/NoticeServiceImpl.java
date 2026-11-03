package com.seinfo.seinfomationA.component.board.service.impl;

import com.seinfo.seinfomationA.util.security.ResourceAccess;
import com.seinfo.seinfomationA.util.security.SafeContent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seinfo.seinfomationA.component.board.Model.NoticeModel;
import com.seinfo.seinfomationA.component.board.mapper.NoticeMapper;
import com.seinfo.seinfomationA.component.board.service.NoticeService;
import com.seinfo.seinfomationA.component.file.mapper.TusMapper;
import com.seinfo.seinfomationA.component.file.service.TusService;
import com.seinfo.seinfomationA.util.ComUtil;
import com.seinfo.seinfomationA.util.response.CoreWebApiResponseModel;
import com.seinfo.seinfomationA.util.security.AuthUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeServiceImpl implements NoticeService {
    private final ResourceAccess resourceAccess;


    private final TokenUtils tokenUtils;
    private final ComUtil comUtil;
    private final NoticeMapper noticeMapper;
    private final TusMapper tusMapper;
    private final TusService tusService;
    private final AuthUtil authUtil;

    /**
     * 페이지 호출 : 공지사항 (외부:LRTP) / (내부:RTP)
     */
    @SuppressWarnings("unchecked")
    public ModelAndView loadNoticeBoardPage(int page, String keyword, String searchType, String noticeNo) {
        boolean adminYn = false;

        Map<String, Object> param = new HashMap<>();
        ModelAndView mv = new ModelAndView();
        String userType = Objects.toString(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_TYPE), "USER");
        mv.addObject("userType", userType);

        // 내부 : 토큰 유효성 체크 + 권한 조회
        if (authUtil.isRoleInTokenAndAuth("ADMIN")) { //최초 페이지 호출시, 1번만.
            adminYn = true;
        }
        mv.setViewName("seinfo/board/noticeList");

        //전체 레코드 수 조회
        param.put("keyword", keyword);
        param.put("searchType", searchType);
        int totalRecords = noticeMapper.getNoticeListCount(param);

        //페이지 정보 계산
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); //최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); //현재 페이지 범위 제한
        int startRow = (cPage - 1) * pageSize;

        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> noticeList = noticeMapper.selectNoticeList(param);

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalRecords", totalRecords);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        mv.addObject("noticeList", noticeList);
        mv.addObject("pageResult", pageResult);
        mv.addObject("page", page);
        mv.addObject("keyword", keyword);
        mv.addObject("searchType", searchType);
        mv.addObject("noticeNo", noticeNo);
        mv.addObject("adminYn", adminYn);

        return mv;

    }

    /**
     * 공지사항 목록 재조회
     */
    @Override
    public ResponseEntity<?> reloadNoticeList(Map<String, Object> param){
        Map<String, Object> resultMap = new HashMap<>();

        // 전체 레코드 수 조회
        int totalRecords = noticeMapper.getNoticeListCount(param);

        // 페이지 정보 계산
        int page = Integer.parseInt(param.get("page").toString());
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); // 최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); // 현재 페이지 범위 제한
        int startRow = (cPage -1) * pageSize;

        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> noticeList = noticeMapper.selectNoticeList(param);

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalRecords", totalRecords);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        resultMap.put("noticeList", noticeList);
        resultMap.put("pageResult", pageResult);

        return ResponseEntity.ok(resultMap);
    }

    /**
     * 공지사항 입력 : 팝업화면
     */
    @SuppressWarnings("unchecked")
    @Override
    public ResponseEntity<?> registNotice(NoticeModel noticeModel) {
        Map<String, Object> param = new ObjectMapper().convertValue(noticeModel, Map.class);
        Map<String, Object> rtnResult = new HashMap<String, Object>();
        rtnResult.put("message", "");

        //저장전 체크
        if(Objects.isNull(param.get("noticeTitle")) || "".equals(param.get("noticeTitle"))) {
            rtnResult.put("message", "제목을 입력하세요.");
        } else if (Objects.isNull(param.get("noticeContents")) || "".equals(param.get("noticeContents"))) {
            rtnResult.put("message", "내용을 입력하세요.");
        }
        if(!authUtil.isRoleInTokenAndAuth("ADMIN")) {
            rtnResult.put("message", "메뉴 권한이 없습니다. adminYn 상태를 확인하세요.");
        }

        if(!rtnResult.get("message").toString().isEmpty()) {
            return ResponseEntity.badRequest().body(rtnResult);
        }

        //첨부파일 그룹ID 체크
        String mfileGrpId = param.get("fileGrpId").toString();
        try {
            if (!mfileGrpId.isEmpty()) {
                if (tusMapper.selectMfileListCnt(Map.of("mfileGrpId", mfileGrpId)) == 0) {
                    param.replace("fileGrpId", null);
                }
            }
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("registNotice mfileGrpId : {}", e.getMessage());
            rtnResult.put("message", "요청을 처리할 수 없습니다. 입력값과 접근 권한을 확인해주세요.");
            return ResponseEntity.badRequest().body(rtnResult);
        }

        //입력값 설정
        param.put("userId", tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID));

        //입력쿼리
        try{
            if(Objects.equals("Y", param.get("modifyYn"))) {
                noticeMapper.updateNotice(param); //수정
            } else {
                noticeMapper.insertNotice(param); //등록
            }
        } catch (Exception e) {
            if(e instanceof org.springframework.security.access.AccessDeniedException) throw (org.springframework.security.access.AccessDeniedException)e;
            log.error("registNotice : {}", e.getMessage());
            rtnResult.put("message", "서버 내 오류가 발생하였습니다.");
            return ResponseEntity.internalServerError().body(rtnResult);
        }

        rtnResult.put("success", "success");
        return ResponseEntity.ok(rtnResult);
    }

    /**
     * 공지사항 상세조회 : 팝업화면
     */
    @Override
    public ResponseEntity<?> getNoticeDetail(Map<String, Object> param) {
        Map<String, Object> resultMap = noticeMapper.selectNoticeDetail(param);

        if(!Objects.equals("Y", param.get("modifyYn"))) { //수정모드 일 때
            int inquireCnt = Integer.parseInt(resultMap.get("inquireCnt").toString()) + 1;
            param.put("inquireCnt", inquireCnt);
            resultMap.replace("inquireCnt", inquireCnt);
            noticeMapper.updateInquireCnt(param);
        }
        return ResponseEntity.ok(Map.of("noticeDetail", resultMap));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteNotice(Map<String, Object> param) throws Exception {
        resourceAccess.requireAdmin();

        Map<String, Object> rtnResult = new HashMap<String, Object>();
        param.put("userId", this.tokenUtils.getClaimsByValue("USER_ID"));

        //첨부파일 key 조회
        Map<String, Object> mfileGrpIdMap = noticeMapper.selectNoticeFileGrpId(param);

        //seinfo_notice 데이터 삭제
        int dCnt = noticeMapper.deleteNotice(param);
        if(dCnt == 0) {
            String msg = "삭제 처리 중 서버 내 오류가 발생하였습니다. (공지사항 삭제: 0건)";
            log.error("deleteNotice :: deleteNotice {}", msg);
            throw new RuntimeException(msg);
        }

        //물리적 파일 및 DB 삭제
        if(mfileGrpIdMap != null) { //mfileGrpId  존재시
            List<Map<String, Object>> dataList = tusMapper.selectMfileListClient(mfileGrpIdMap);

            if(!dataList.isEmpty()) { //해당 게시물의 파일리스트 존재시
                dataList.forEach(list -> {
                    list.put("fileGrpId", mfileGrpIdMap.get("mfileGrpId"));
                });

                ResponseEntity<?> rtn = tusService.deleteFile(dataList);

                CoreWebApiResponseModel resultModel = (CoreWebApiResponseModel) rtn.getBody();
                String processCode = resultModel.getProcess_code();

                if (!"00".equals(processCode)) {
                    // data 필드는 Object이므로, Map으로 변환 가능한 경우에만 캐스팅
                    Object dataObj = resultModel.getData();
                    if (dataObj instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        String errorMsg = (String) data.get("message");

                        String msg = "삭제 중 서버 내 오류가 발생하였습니다. (파일삭제) <br> ▷ " + errorMsg;
                        log.error("removeProposal >>> deleteFile >>> message : " + msg);
                        throw new RuntimeException(msg);
                    } else {
                        log.error("removeProposal >>> deleteFile >>> data 타입이 Map이 아님: {}", dataObj.getClass());
                        throw new RuntimeException("삭제 중 서버 내 오류가 발생하였습니다. (파일삭제: data 형식 오류)");
                    }
                }
            }
        }
        rtnResult.put("success", "success");
        return ResponseEntity.ok(rtnResult);
    }
}
