package com.example.dogo.controller.Support;

import com.example.dogo.service.Support.InquiryService;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.dto.Support.InquirySummary;
import com.example.dogo.dto.Support.InquiryDetail;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping({"/inquiry", "/inqiry"})
    public String inquiryList(@RequestParam(required = false, defaultValue = "all") String viewMode,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "0") int page,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        
        // viewMode에 따라 본인 글만 볼지, 전체를 볼지 결정
        Page<InquirySummary> inquiries = inquiryService.getInquiryPage(
                viewMode, status, page, 5, 
                userDetails != null ? userDetails.getUser() : null, 
                isAdmin);

        model.addAttribute("currentUri", "/inquiry");
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("viewMode", viewMode);
        model.addAttribute("currentStatus", status == null || status.isEmpty() ? "전체" : status);
        model.addAttribute("isAdmin", isAdmin);

        int blockLimit = 5;
        int startPage = (((int)(Math.ceil((double)(page + 1) / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), inquiries.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "inquiry/list";
    }

    @GetMapping({"/inquiry/new", "/inqiry/new"})
    public String inquiryForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUri", "/inquiry");

        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        
        return "inquiry/inquiry";
    }

    @GetMapping({"/inquiry/{id}", "/inqiry/{id}"})
    public String inquiryDetail(@PathVariable Long id, 
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        
        try {
            InquiryDetail detail = inquiryService.getInquiryDetail(
                    id, 
                    userDetails != null ? userDetails.getUser() : null, 
                    isAdmin);
            
            model.addAttribute("currentUri", "/inquiry");
            model.addAttribute("inquiry", detail);
            model.addAttribute("isAdmin", isAdmin);
            return "inquiry/detail";
        } catch (IllegalArgumentException e) {
            model.addAttribute("message", e.getMessage());
            return "inquiry/error"; 
        }
    }

    @PostMapping({"/inquiry", "/inqiry"})
    public String createInquiry(@RequestParam String category,
                                @RequestParam String title,
                                @RequestParam String content,
                                @RequestParam(value = "files", required = false) org.springframework.web.multipart.MultipartFile[] files,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        java.util.List<org.springframework.web.multipart.MultipartFile> fileList = 
                (files != null) ? java.util.Arrays.asList(files) : null;
        
        inquiryService.create(category, title, content, fileList, 
                userDetails != null ? userDetails.getUser() : null);
        
        return "redirect:/inquiry";
    }

    @PostMapping("/admin/inquiry/{id}/answer")
    public String answerInquiry(@PathVariable Long id, @RequestParam String answer) {
        inquiryService.answer(id, answer);
        return "redirect:/inquiry/" + id;
    }
}
