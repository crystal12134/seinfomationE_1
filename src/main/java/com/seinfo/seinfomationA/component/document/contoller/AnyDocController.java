package com.seinfo.seinfomationA.component.document.contoller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/anyone")
@RequiredArgsConstructor
@Slf4j
public class AnyDocController {

    @GetMapping(value = "/docAgree")
    public ModelAndView agreePage() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/document/pageAgree");
        return mv;
    }

    @GetMapping(value = "/docPolicy")
    public ModelAndView policyPage() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("seinfo/document/pagePolicy");
        return mv;
    }
}
