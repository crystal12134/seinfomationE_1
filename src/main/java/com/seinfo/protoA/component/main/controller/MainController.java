package com.seinfo.protoA.component.main.controller;

import com.seinfo.protoA.component.main.service.MainService;
import com.seinfo.protoA.util.CookieUtil;
import com.seinfo.protoA.util.response.CodeCoreResultList;
import com.seinfo.protoA.util.response.ResponseEntityUtil;
import com.seinfo.protoA.util.token.TokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("")
public class MainController {

    private final MainService mainService;

    /**
     * 페이지호출 : 메인
     */
    @GetMapping(value = "")
    public ModelAndView anyonePage() {
        ModelAndView mv = mainService.mainPage();
        mv.setViewName("seinfo/main/index");
        return mv;
    }

    /**
     * 에러페이지
     */

    @GetMapping("/error/accessDenied")
    public ModelAndView accessDeniedPage() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/error/accessDenied");
        return mv;
    }
}
