package com.example.dogo.controller.Support;

import com.example.dogo.service.Support.InquiryService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/*1:1 문의하기 페이지와 관련된 웹 요청을 처리*/
@Controller
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @GetMapping({"/inquiry", "/inqiry"})
    public String inquiryList(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        Page<InquiryService.InquirySummary> inquiries = inquiryService.getInquiryPage(category, status, page, 5);

        model.addAttribute("currentUri", "/inquiry");
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("currentCategory", category == null || category.isEmpty() ? "전체" : category);
        model.addAttribute("currentStatus", status == null || status.isEmpty() ? "전체" : status);

        /* 
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", true); // true:관리자 false:사용자

        int blockLimit = 5;
        int startPage = (((int)(Math.ceil((double)(page + 1) / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), inquiries.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "inquiry/list";
    }

    @GetMapping({"/inquiry/new", "/inqiry/new"})
    public String inquiryForm(Model model) {
        model.addAttribute("currentUri", "/inquiry");

        /*
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", true); // true:관리자 false:사용자
        return "inquiry/inquiry";
    }

    @GetMapping({"/inquiry/{id}", "/inqiry/{id}"})
    public String inquiryDetail(@PathVariable Long id, Model model) {
        model.addAttribute("currentUri", "/inquiry");
        model.addAttribute("inquiry", inquiryService.getInquiryDetail(id));

        /*
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", true); // true:관리자 false:사용자
        return "inquiry/detail";
    }

    @PostMapping({"/inquiry", "/inqiry"})
    public String createInquiry(@RequestParam String category,
                                @RequestParam String title,
                                @RequestParam String content,
                                @RequestParam(value = "files", required = false) org.springframework.web.multipart.MultipartFile[] files) {
        java.util.List<org.springframework.web.multipart.MultipartFile> fileList = 
                (files != null) ? java.util.Arrays.asList(files) : null;
        inquiryService.create(category, title, content, fileList);
        return "redirect:/inquiry";
    }

    @PostMapping({"/inquiry/{id}/answer", "/inqiry/{id}/answer"})
    public String answerInquiry(@PathVariable Long id, @RequestParam String answer) {
        inquiryService.answer(id, answer);
        return "redirect:/inquiry/" + id;
    }
}
