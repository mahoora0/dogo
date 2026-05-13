package com.example.dogo.controller;

import com.example.dogo.service.InquiryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/*1:1 문의하기 페이지와 관련된 웹 요청을 처리하는 컨트롤러 클래스입니다.*/
@Controller
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping({"/inquiry", "/inqiry"})
    public String inquiryList(Model model) {
        model.addAttribute("currentUri", "/inquiry");
        model.addAttribute("inquiryGroups", inquiryService.groupedInquiries());
        model.addAttribute("inquiries", inquiryService.getInquiries());
        return "inquiry/list";
    }

    @GetMapping({"/inquiry/new", "/inqiry/new"})
    public String inquiryForm(Model model) {
        model.addAttribute("currentUri", "/inquiry");
        return "inquiry/inquiry";
    }

    @GetMapping({"/inquiry/{id}", "/inqiry/{id}"})
    public String inquiryDetail(@PathVariable Long id, Model model) {
        model.addAttribute("currentUri", "/inquiry");
        model.addAttribute("inquiry", inquiryService.getInquiryDetail(id));
        return "inquiry/detail";
    }

    @PostMapping({"/inquiry", "/inqiry"})
    public String createInquiry(@RequestParam String category,
                                @RequestParam String title,
                                @RequestParam String content) {
        inquiryService.create(category, title, content);
        return "redirect:/inquiry";
    }
}
