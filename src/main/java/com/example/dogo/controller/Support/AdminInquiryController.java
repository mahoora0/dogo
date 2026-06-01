package com.example.dogo.controller.Support;

import com.example.dogo.service.Support.InquiryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminInquiryController {

    private final InquiryService inquiryService;

    public AdminInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping("/admin/inquiries")
    public String list(Model model) {
        model.addAttribute("currentUri", "/admin/inquiries");
        model.addAttribute("inquiries", inquiryService.getAdminInquiries());
        return "admin/inquiries/list";
    }

    @GetMapping({"/admin/inquiries/{id}", "/admin/inquiry/{id}"})
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("currentUri", "/admin/inquiries");
        model.addAttribute("inquiry", inquiryService.getAdminInquiryDetail(id));
        return "admin/inquiries/detail";
    }

    @PostMapping("/admin/inquiries/{id}/answer")
    public String answer(@PathVariable Long id, @RequestParam String answer) {
        inquiryService.answer(id, answer);
        return "redirect:/admin/inquiries/" + id;
    }
}
