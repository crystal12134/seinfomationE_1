package com.seinfo.seinfomationA.component.board.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seinfo.seinfomationA.component.board.mapper.QnaMapper;
import com.seinfo.seinfomationA.component.board.Model.QnaModel;
import com.seinfo.seinfomationA.component.board.service.QnaService;
import com.seinfo.seinfomationA.util.response.CodeCoreResultList;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.security.AuthUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QnaServiceImpl implements QnaService {

    private final TokenUtils tokenUtils;
    private final ResponseEntityUtil responseEntityUtil;
    private final QnaMapper qnaMapper;
    private final AuthUtil authUtil;

    /**
     * 페이지호출 : QnA (외부:LRTP) / (내부:RTP)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView loadQnaPage(int page, String keyword) {
        String adminYn = "N";

        Map<String, Object> param = new HashMap<>();
        ModelAndView mv = new ModelAndView();

        String userType = Objects.toString(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_TYPE).toString(), "");
        mv.addObject("userType", userType);

        String tokenUserId =  Objects.toString(tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID), "");
        //내부 : 권한 조회
        if("ADMIN".equals(userType)){
            // 정상: "Y"
            adminYn = "Y";
            param.put("userEmail", ""); //내부: 현업관리자는 모두 조회
        }else {
            param.put("userEmail", tokenUserId); // 외부 : 자신것만 조회
        }

        mv.setViewName("seinfo/board/qnaList");

        // 전체 레코드 수 조회
        param.put("keyword", keyword);
        param.put("userType", userType);
        int totalRecords = qnaMapper.getQnaListCount(param);

        //페이지 정보 계산
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); //최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); // 현재 페이지 범위 제한
        int startRow = (cPage -1) * pageSize;

        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> qnaList = qnaMapper.selectQnaList(param);

        Map<String, Object> pageResult = new HashMap<String, Object>();
        pageResult.put("cPage",cPage);
        pageResult.put("totalRecords", totalRecords);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        mv.addObject("qnaList", qnaList);
        mv.addObject("pageResult", pageResult);
        mv.addObject("keyword", keyword);
        mv.addObject("page", page);
        mv.addObject("adminYn", adminYn);

        return mv;
    }

    /**
     *  QnA 목록 재조회
     */
    @Override
    public ResponseEntity<?> reloadQnaPage(Map<String, Object> param) {
        Map<String, Object> rtnResult = new HashMap<String, Object>();

        //조회전 체크
        String userEmail = Objects.toString(this.tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID), "");
        if(Objects.isNull(param.get("userType")) || "".equals(param.get("userType")) || userEmail.isBlank()) {
            rtnResult.put("message", "메뉴 권한이 없습니다. userType을 확인하세요.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", rtnResult);
        }
        param.put("userEmail", userEmail);


        // 전체 레코드 수 조회
        int totalRecords = qnaMapper.getQnaListCount(param);

        // 페이지 정보 계산
        int page = Integer.parseInt(param.get("page").toString());
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); //최소 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); // 현재 페이지 범위 제한
        int startRow = (cPage - 1) * pageSize;

        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> qnaList = qnaMapper.selectQnaList(param);

        Map<String, Object> pageResult = new HashMap<String, Object>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalRecords", totalRecords);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        rtnResult.put("qnaList", qnaList);
        rtnResult.put("pageResult", pageResult);

        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", rtnResult);
    }

    /**
     *  QnA 입력(문의하기) : 팝업화면 (외부:LRTP)
     */

    @SuppressWarnings("unchecked")
    @Override
    public ResponseEntity<?> registQuestion(QnaModel qnaModel) {
        Map<String, Object> param = new ObjectMapper().convertValue(qnaModel, Map.class);
        Map<String, Object> rtnResult = new HashMap<String, Object>();
        rtnResult.put("message", "");

        //저장전 체크
        if(Objects.isNull(param.get("userType")) || "".equals(param.get("userType"))) {
            rtnResult.put("message", "메뉴 권한이 없습니다. userType 상태를 확인하세요.");
        }
        if(Objects.isNull(param.get("userType")) || "".equals(param.get("adminYn"))) {
            rtnResult.put("message", "메뉴 권한이 없습니다. adminYn 상태를 확인하세요.");
        }

        //문의하기 권한 : 외부 + admin 아닐경우
        if(!("USER".equals(param.get("userType")) && "N".equals(param.get("adminYn")))) {
            rtnResult.put("message", "1:1문의를 등록할 권한이 없습니다.");
        }
        if(Objects.isNull(param.get("modifyMode")) || "".equals(param.get("modifyMode"))) {
            rtnResult.put("message", "메뉴 권한이 없습니다. modifyMode 상태를 확인하세요");
        }
        if("RU".equals(param.get("modifyMode")) && "".equals(param.get("noticeNo"))) {
            rtnResult.put("message", "1:1문의 수정 중 오류발생 하였습니다. (key:noticeNo");
        }
        if("RD".equals(param.get("modifyMode")) && "".equals(param.get("noticeNo"))) {
            rtnResult.put("message", "1:1문의 삭제 중 오류발생 하였습니다. (key:noticeNo");
        }
        if(!"RD".equals(param.get("modifyMode"))) {
            if(Objects.isNull(param.get("noticeTitle")) || "".equals(param.get("noticeTitle"))) {
                rtnResult.put("message", "제목을 입력하세요.");
            } else if(Objects.isNull(param.get("noticeContents")) || "".equals(param.get("noticeContents"))) {
                rtnResult.put("message", "내용을 입력하세요.");
            }
        }
        if(!rtnResult.get("message").toString().isEmpty()) {
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", rtnResult);
        }

        //입력값 설정
        param.put("userEmail", this.tokenUtils.getClaimsByValue("USER_ID"));

        try{
            if("R".equals(param.get("modifyMode"))) {
                qnaMapper.insertQna(param);
            } else if ("RU".equals(param.get("modifyMode"))) {
                qnaMapper.updateQna(param);
            } else if ("RD".equals(param.get("modifyMode"))) {
                if("Y".equals(param.get("replyYn"))) { //답변 존재시 : 삭제되었습니다 로 update
                    param.put("noticeTitle", "삭제 되었습니다.");
                    param.put("noticeContents", "삭제 되었습니다.");
                    qnaMapper.updateQna(param);
                }else {
                    qnaMapper.updateQnaDel(param); //답변 없음 : delete
                }
            }
        } catch (Exception e) {
            log.error("registQuestion : {}", e.getMessage());
            rtnResult.put("message", "1:1문의를 등록 중 서버내 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", rtnResult);
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "정상적으로 처리되었습니다.");
    }

    /**
     *  QnA 입력 (답변하기) : 팝업화면 (내부:RTP)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ResponseEntity<?> registReply(QnaModel qnaModel) {
        Map<String, Object> param = new ObjectMapper().convertValue(qnaModel, Map.class);
        Map<String, Object> rtnResult = new HashMap<String, Object>();
        rtnResult.put("message", "");

        //저장전 체크
        if (Objects.isNull(param.get("userType")) || "".equals(param.get("userType"))) {
            rtnResult.put("message", "메뉴 권한이 없습니다. userType 상태를 확인하세요.");
        }
        if (Objects.isNull(param.get("adminYn")) || "".equals(param.get("adminYn"))) {
            rtnResult.put("message", "메뉴 권한이 없습니다. adminYn 상태를 확인하세요.");
        }
        //답변하기 권한 : 내부 + admin 일때만.
        if (!authUtil.isRoleInTokenAndAuth("ADMIN")) {
            rtnResult.put("message", "1:1문의에 대한 답변을 등록할 권한이 없습니다.");
        }
        if (Objects.isNull(param.get("modifyMode")) || "".equals(param.get("modifyMode"))) {
            rtnResult.put("message", "메뉴 권한이 없습니다. modifyMode 상태를 확인하세요.");
        }
        if ("AD".equals(param.get("modifyMode")) && "".equals(param.get("noticeNo"))) {
            rtnResult.put("message", "1:1문의 답변 삭제 중 오류발생 하였습니다. (key:noticeNo");
        }
        if (!"AD".equals(param.get("modifyMode"))) {
            if (Objects.isNull(param.get("answerTitle")) || "".equals(param.get("answerTitle"))) {
                rtnResult.put("message", "제목을 입력하세요.");
            } else if (Objects.isNull(param.get("answerContents")) || "".equals(param.get("answerContents"))) {
                rtnResult.put("message", "내용을 입력하세요.");
            }
        }
        if (!rtnResult.get("message").toString().isEmpty()) {
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", rtnResult);
        }

        //입력값 설정
        param.put("userId", this.tokenUtils.getClaimsByValue("USER_ID"));

        if (!"A".equals(param.get("modifyMode"))) {
            param.put("answerUserId", this.tokenUtils.getClaimsByValue("USER_ID"));
        }
        try {
            if ("AD".equals(param.get("modifyMode"))) {
                if ("Y".equals(param.get("qnaDelYn"))) { //질문삭제 상태일때 : 답변삭제시 delete
                    qnaMapper.updateQnaDel(param);
                } else {
                    qnaMapper.updateAnswerDel(param); //질문 존재시 : 답변만 clear
                }
            } else {
                qnaMapper.updateAnswer(param);
            }
        } catch (Exception e) {
            log.error("registReply : {}", e.getMessage());
            rtnResult.put("message", "답변 등록 중 서버내 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", rtnResult);
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "정상적으로 처리되었습니다.");
    }

    /**
     *  QnA 상세조회 (문의하기), (답변하기) : 팝업화면
     */
    @Override
    public ResponseEntity<?> getQuestionDetail(Map<String, Object> param) {
        Map<String, Object> rtnResult = qnaMapper.selectQnaDetail(param);

        String userId = (String) this.tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID);
        String userType = (String) this.tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_TYPE);

        //등록자 이거나 권한이 있는 경우
        if(rtnResult.get("userEmail").equals(userId) || "ADMIN".equals(userType)) {
                if(rtnResult.get("answerUserId") == null) {
                    rtnResult.put("answerUserId", this.tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_ID));
                    rtnResult.putIfAbsent("answerUserNm", this.tokenUtils.getClaimsByValue(TokenUtils.TOKEN_USER_NM));
                }
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", Map.of("qnaDetail", rtnResult));
        }else {
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", Map.of("message", "등록자가 다릅니다."));
        }
    }
}

