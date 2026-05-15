package com.example.dogo.controller.Support;

import com.example.dogo.entity.Support.Notice;
import com.example.dogo.service.Support.NoticeService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    // [User] 사용자용 목록
    @GetMapping("/notice")
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Notice> notices = noticeService.getNotices(category, page, 5);
        
        model.addAttribute("currentUri", "/notice");
        model.addAttribute("notices", notices);
        model.addAttribute("currentCategory", category == null || category.isEmpty() ? "전체" : category);
        model.addAttribute("isAdmin", false);

        int blockLimit = 5;
        int startPage = (((int)(Math.ceil((double)(page + 1) / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), notices.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "notice/list";
    }

    // [Admin] 관리자용 목록
    @GetMapping("/admin/notice")
    public String adminList(@RequestParam(required = false) String category,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        Page<Notice> notices = noticeService.getNotices(category, page, 5);
        
        model.addAttribute("currentUri", "/admin/notice");
        model.addAttribute("notices", notices);
        model.addAttribute("currentCategory", category == null || category.isEmpty() ? "전체" : category);
        model.addAttribute("isAdmin", true);

        int blockLimit = 5;
        int startPage = (((int)(Math.ceil((double)(page + 1) / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), notices.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "notice/list";
    }

    // [User] 사용자용 상세
    @GetMapping("/notice/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getNoticeDetail(id);
        model.addAttribute("currentUri", "/notice");
        model.addAttribute("notice", notice);
        model.addAttribute("isAdmin", false);
        return "notice/detail";
    }

    // [Admin] 관리자용 상세 (수정/삭제 버튼 노출)
    @GetMapping("/admin/notice/{id}")
    public String adminDetail(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getNoticeDetail(id);
        model.addAttribute("currentUri", "/admin/notice");
        model.addAttribute("notice", notice);
        model.addAttribute("isAdmin", true);
        return "notice/detail";
    }

    // [Admin] 등록 폼
    @GetMapping("/admin/notice/new")
    public String form(Model model) {
        model.addAttribute("currentUri", "/admin/notice");
        model.addAttribute("notice", new Notice());
        model.addAttribute("isAdmin", true);
        return "notice/form";
    }

    // [Admin] 등록 처리
    @PostMapping("/admin/notice")
    public String create(@RequestParam String title,
                         @RequestParam String category,
                         @RequestParam String content,
                         @RequestParam(required = false, defaultValue = "관리자") String writer) {
        noticeService.createNotice(title, category, content, writer);
        return "redirect:/admin/notice";
    }

    // [Admin] 수정 폼
    @GetMapping("/admin/notice/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Notice notice = noticeService.getNoticeDetail(id);
        model.addAttribute("currentUri", "/admin/notice");
        model.addAttribute("notice", notice);
        model.addAttribute("isAdmin", true);
        return "notice/form";
    }

    // [Admin] 수정 처리
    @PostMapping("/admin/notice/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String category,
                       @RequestParam String content) {
        noticeService.updateNotice(id, title, category, content);
        return "redirect:/admin/notice/" + id;
    }

    // [Admin] 삭제 처리
    @PostMapping("/admin/notice/{id}/delete")
    public String delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/admin/notice";
    }
}
