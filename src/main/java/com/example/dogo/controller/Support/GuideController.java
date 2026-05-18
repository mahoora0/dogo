package com.example.dogo.controller.Support;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GuideController {

    @GetMapping("/guide")
    public String list(@RequestParam(required = false) String category, Model model) {
        // 기본값을 '분실자' 카테고리로 지정
        if (category == null || category.isEmpty()) {
            category = "분실자";
        }
        model.addAttribute("currentCategory", category);
        return "guide/list";
    }
}
