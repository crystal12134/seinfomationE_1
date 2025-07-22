package com.seinfo.protoA.component.board.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seinfo.protoA.component.board.mapper.FaqMapper;
import com.seinfo.protoA.component.board.Model.FaqModel;
import com.seinfo.protoA.component.board.service.FaqService;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaqServiceImpl implements FaqService {

    private final FaqMapper faqMapper;
    private final ResponseEntityUtil responseEntityUtil;

    @Override
    public ModelAndView loadFaqListPage(Integer faqNo, int page, String faqType) {
        Map<String, Object> param = new HashMap<>();
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/board/faqList");

        int pageSize = 10;
        param.put("faqType", faqType);

        //(1) faqNo가 있을 때, 페이지세팅
        if(faqNo != null){
            param.put("faqNo", faqNo);
            int rnum = faqMapper.selectFaqRnum(param);
            if(rnum != 0) {
                page = (rnum - 1) / pageSize + 1;
            }
        }

        //(2) 전체 레코드 수 조회
        int totalRecords = faqMapper.getFqaListCount(param);

        //(3) 페이지 정보 계산
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize)); // 최서 1페이지 보장
        int cPage = Math.max(1, Math.min(page, totalPages)); //현재 페이지 범위 제한
        int startRow = (cPage -1) * pageSize;

        //(4) FAQ 리스트 조회
        param.put("startRow", startRow);
        param.put("pageSize", pageSize);
        List<Map<String, Object>> faqList = faqMapper.selectFaqList(param);

        //(5) 페이지 결과 세팅
        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("cPage", cPage);
        pageResult.put("totalRecords", totalRecords);
        pageResult.put("totalPages", totalPages);
        pageResult.put("hPrev", cPage > 1);
        pageResult.put("hNext", cPage < totalPages);

        //(6) 화면에 전달
        mv.addObject("faqList", faqList);
        mv.addObject("pageResult", pageResult);
        mv.addObject("page", page);
        mv.addObject("faqType", faqType);
        mv.addObject("faqNo", faqNo);

        return mv;
    }

    /**
     *  FAQ 목록 조회
     */
    @SuppressWarnings("unchecked")
    @Override
    public ResponseEntity<?> registFaq(FaqModel faqModel) {
        Map<String, Object> param = new ObjectMapper().convertValue(faqModel, Map.class);
        Map<String, Object> errorResult = new HashMap<String, Object>();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            errorResult.put("message", "로그인이 필요합니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
        String userEmail = authentication.getName();
        param.put("userEmail", userEmail);

        try{
            faqMapper.insertFqa(param);
        }catch (Exception e) {
            log.error("registNotice : {}", e.getMessage());
            errorResult.put("message", "서버 내 오류가 발생하였습니다.");
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", errorResult);
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "정상적으로 처리되었습니다.");
    }

    /**
     * FAQ 수정 (조회건수 증가)
     */
    @Override
    public ResponseEntity<?> updateInquireCnt(Map<String, Object> param) {
        try{
            faqMapper.updateInquireCnt(param);
        }catch (Exception e) {
            log.error("updateInquireCnt : {}", e.getMessage());
            return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_FAILED, "E001", Map.of("message","서버내 오류가 발생하였습니다."));
        }
        return this.responseEntityUtil.getResponseEntity(CodeCoreResultList.RESULT_CODE_SUCCESS, "S001", "정상적으로 처리되었습니다.");
    }
}
