package com.seinfo.seinfomationA.component.main.service.impl;

import com.seinfo.seinfomationA.component.main.mapper.LoginMapper;
import com.seinfo.seinfomationA.component.main.mapper.MainMapper;
import com.seinfo.seinfomationA.component.main.service.MainService;
import com.seinfo.seinfomationA.util.response.ResponseEntityUtil;
import com.seinfo.seinfomationA.util.token.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
