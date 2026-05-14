package com.example.dogo.controller.Support;

import com.example.dogo.service.Support.FAQService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FAQController {

    private final FAQService faqService;

    public FAQController(FAQService faqService) {
        this.faqService = faqService;
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("currentUri", "/faq"); 
        model.addAttribute("faqList", faqService.getActiveFAQs());
        /*
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", false); // true:관리자 false:사용자
        return "FAQ/FAQ"; 
    }

    @GetMapping("/faq/new")
    public String faqForm(Model model) {
        model.addAttribute("currentUri", "/faq");
        /*
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", false); // true:관리자 false:사용자
        return "FAQ/FAQ-write";
    }

    @PostMapping("/faq")
    public String createFaq(@RequestParam String category, 
                            @RequestParam String question, 
                            @RequestParam String answer) {
        faqService.createFAQ(category, question, answer);
        return "redirect:/faq";
    }

    @PostMapping("/faq/{id}/delete")
    public String deleteFaq(@org.springframework.web.bind.annotation.PathVariable Long id) {
        faqService.deleteFAQ(id);
        return "redirect:/faq";
    }
}
