package com.example.dogo.controller.Support;

import com.example.dogo.service.Support.FAQService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
public class FAQController {

    private final FAQService faqService;

    public FAQController(FAQService faqService) {
        this.faqService = faqService;
    }

    // [User] 일반 사용자용 목록 (관리 버튼 없음)
    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("currentUri", "/faq"); 
        model.addAttribute("faqList", faqService.getActiveFAQs());
        model.addAttribute("isAdmin", false); // 사용자 페이지이므로 무조건 false
        return "FAQ/FAQ"; 
    }

    // [Admin] 관리자 전용 목록 (관리 버튼 노출)
    @GetMapping("/admin/faq")
    public String adminFaq(Model model) {
        model.addAttribute("currentUri", "/admin/faq");
        model.addAttribute("faqList", faqService.getActiveFAQs());
        model.addAttribute("isAdmin", true); // 관리자 페이지이므로 무조건 true
        return "admin/FAQ/FAQ"; 
    }

    // [Admin] 등록 화면
    @GetMapping("/admin/faq/new")
    public String faqForm(Model model) {
        model.addAttribute("currentUri", "/admin/faq");
        model.addAttribute("isAdmin", true);
        return "admin/FAQ/FAQ-write";
    }

    // [Admin] 등록 처리
    @PostMapping("/admin/faq")
    public String createFaq(@RequestParam String category, 
                            @RequestParam String question, 
                            @RequestParam String answer) {
        faqService.createFAQ(category, question, answer);
        return "redirect:/admin/faq"; // 등록 후 관리자 목록으로 이동
    }

    // [Admin] 삭제 처리
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/faq/{id}/delete")
    public String deleteFaq(@PathVariable Long id) {
        faqService.deleteFAQ(id);
        return "redirect:/admin/faq"; // 삭제 후 관리자 목록으로 이동
    }
}
