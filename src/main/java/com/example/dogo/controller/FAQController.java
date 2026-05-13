package com.example.dogo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/* FAQ(자주 묻는 질문) 페이지와 관련된 웹 요청을 처리하는 컨트롤러 클래스입니다.*/

@Controller
public class FAQController {


    @GetMapping("/faq")
    public String faq(Model model) {
        // 사이드바 메뉴 중 'FAQ' 항목을 활성화 상태로 표시하기 위해 현재 URI를 뷰로 전달합니다.
        model.addAttribute("currentUri", "/faq");
        return "FAQ/FAQ";
    }

    @GetMapping("/notice")
    public String notice(Model model) {
        model.addAttribute("currentUri", "/notice");
        return "notice/list";
    }

    @GetMapping("/guide")
    public String guide(Model model) {
        model.addAttribute("currentUri", "/guide");
        return "guide/index";
    }
}
