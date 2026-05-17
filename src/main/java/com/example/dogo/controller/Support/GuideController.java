package com.example.dogo.controller.Support;

import com.example.dogo.entity.Support.Guide;
import com.example.dogo.service.Support.GuideService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class GuideController {

    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    // [User] 사용자용 목록
    @GetMapping("/guide")
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Guide> guides = guideService.getGuides(category, page, 5);
        
        model.addAttribute("currentUri", "/guide");
        model.addAttribute("guides", guides);
        model.addAttribute("currentCategory", category == null || category.isEmpty() ? "전체" : category);
        model.addAttribute("isAdmin", false);

        int blockLimit = 5;
        int startPage = (((int)(Math.ceil((double)(page + 1) / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), guides.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "guide/list";
    }

    // [Admin] 관리자용 목록
    @GetMapping("/admin/guide")
    public String adminList(@RequestParam(required = false) String category,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        Page<Guide> guides = guideService.getGuides(category, page, 5);
        
        model.addAttribute("currentUri", "/admin/guide");
        model.addAttribute("guides", guides);
        model.addAttribute("currentCategory", category == null || category.isEmpty() ? "전체" : category);
        model.addAttribute("isAdmin", true);

        int blockLimit = 5;
        int startPage = (((int)(Math.ceil((double)(page + 1) / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), guides.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "guide/list";
    }

    // [User] 사용자용 상세
    @GetMapping("/guide/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Guide guide = guideService.getGuideDetail(id);
        model.addAttribute("currentUri", "/guide");
        model.addAttribute("guide", guide);
        model.addAttribute("isAdmin", false);
        return "guide/detail";
    }

    // [Admin] 관리자용 상세 (수정/삭제 버튼 노출)
    @GetMapping("/admin/guide/{id}")
    public String adminDetail(@PathVariable Long id, Model model) {
        Guide guide = guideService.getGuideDetail(id);
        model.addAttribute("currentUri", "/admin/guide");
        model.addAttribute("guide", guide);
        model.addAttribute("isAdmin", true);
        return "guide/detail";
    }

    // [Admin] 등록 폼
    @GetMapping("/admin/guide/new")
    public String form(Model model) {
        model.addAttribute("currentUri", "/admin/guide");
        model.addAttribute("guide", new Guide());
        model.addAttribute("isAdmin", true);
        return "guide/form";
    }

    // [Admin] 등록 처리
    @PostMapping("/admin/guide")
    public String create(@RequestParam String title,
                         @RequestParam String category,
                         @RequestParam String content,
                         @RequestParam(required = false, defaultValue = "관리자") String writer) {
        guideService.createGuide(title, category, content, writer);
        return "redirect:/admin/guide";
    }

    // [Admin] 수정 폼
    @GetMapping("/admin/guide/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Guide guide = guideService.getGuideDetail(id);
        model.addAttribute("currentUri", "/admin/guide");
        model.addAttribute("guide", guide);
        model.addAttribute("isAdmin", true);
        return "guide/form";
    }

    // [Admin] 수정 처리
    @PostMapping("/admin/guide/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String category,
                       @RequestParam String content) {
        guideService.updateGuide(id, title, category, content);
        return "redirect:/admin/guide/" + id;
    }

    // [Admin] 삭제 처리
    @PostMapping("/admin/guide/{id}/delete")
    public String delete(@PathVariable Long id) {
        guideService.deleteGuide(id);
        return "redirect:/admin/guide";
    }
}
