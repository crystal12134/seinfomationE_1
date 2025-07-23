package com.seinfo.seinfomationA.component.intro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/anyone")
@RequiredArgsConstructor
@Slf4j
public class AnyInfoController {

    @GetMapping(value = "/companyInfo")
    public ModelAndView companyInfoPage() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/intro/companyInfo");
        return mv;
    }

    @GetMapping(value = "/mobilityInfo")
    public ModelAndView mobilityInfoPage() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/intro/mobilityInfo");
        return mv;
    }
}
