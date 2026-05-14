package com.example.dogo.controller;

import com.example.dogo.entity.Notice;
import com.example.dogo.service.NoticeService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Notice> notices = noticeService.getNotices(category, page, 5);
        
        model.addAttribute("currentUri", "/notice");
        model.addAttribute("notices", notices);
        model.addAttribute("currentCategory", category == null || category.isEmpty() ? "전체" : category);

        /*
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", true); // true:관리자, false:사용자

        // 페이지 블록 계산 (예: 5개씩 렌더링)
        int blockLimit = 5;
        int startPage = (((int)(Math.ceil((double)(page + 1) / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), notices.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "notice/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getNoticeDetail(id);
        
        model.addAttribute("currentUri", "/notice");
        model.addAttribute("notice", notice);

        /*
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        */
        model.addAttribute("isAdmin", true); // true:관리자, false:사용자

        return "notice/detail";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("currentUri", "/notice");
        model.addAttribute("notice", new Notice());
        
        /*
        // TODO: 향후 Spring Security 적용 시 아래 코드로 실제 관리자 여부 확인
        */
        model.addAttribute("isAdmin", true);

        return "notice/form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam String category,
                         @RequestParam String content,
                         @RequestParam(required = false, defaultValue = "관리자") String writer) {
        noticeService.createNotice(title, category, content, writer);
        return "redirect:/notice";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getNoticeDetail(id);
        
        model.addAttribute("currentUri", "/notice");
        model.addAttribute("notice", notice);
        model.addAttribute("isAdmin", true);

        return "notice/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String category,
                       @RequestParam String content) {
        noticeService.updateNotice(id, title, category, content);
        return "redirect:/notice/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/notice";
    }
}
