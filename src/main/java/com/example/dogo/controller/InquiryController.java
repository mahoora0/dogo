package com.example.dogo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/*1:1 문의하기 페이지와 관련된 웹 요청을 처리하는 컨트롤러 클래스입니다.*/
@Controller
public class InquiryController {


    @GetMapping("/inquiry")
    public String inquiry(Model model) {
        model.addAttribute("currentUri", "/inquiry");
        return "inquiry/inquiry";
    }
}
