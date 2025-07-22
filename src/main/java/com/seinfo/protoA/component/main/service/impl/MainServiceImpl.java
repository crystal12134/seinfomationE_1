package com.seinfo.protoA.component.main.service.impl;

import com.seinfo.protoA.component.main.mapper.LoginMapper;
import com.seinfo.protoA.component.main.mapper.MainMapper;
import com.seinfo.protoA.component.main.service.MainService;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
import com.seinfo.protoA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainServiceImpl implements MainService {

    private final MainMapper mainMapper;
    private final LoginMapper loginMapper;
    private final ResponseEntityUtil responseEntityUtil;
    private final TokenUtils tokenUtils;

    @Override
    public ModelAndView mainPage() {
        ModelAndView mv = new ModelAndView();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {
            mv.addObject("loginYn", false);
        }else {
            mv.addObject("loginYn", true);
        }

        List<Map<String, Object>> noticeList = mainMapper.selectNoticeList();
        List<Map<String, Object>> faqList = mainMapper.selectFaqList();

        mv.addObject("noticeList", noticeList);
        mv.addObject("faqList", faqList);

        return mv;
    }
}
