package com.example.dogo.controller.Support;

import com.example.dogo.service.Support.FAQService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", true);
        return "FAQ/FAQ"; 
    }

    @GetMapping("/faq/new")
    public String faqForm(Model model) {
        model.addAttribute("currentUri", "/faq");
        /*
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", true);
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
