package com.example.dogo.controller;
 
import com.example.dogo.service.LostItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
 
@Controller
@RequiredArgsConstructor
public class MainController {
 
  private final LostItemService lostItemService;
 
  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("currentUri", "/"); // 메인 페이지 표시
    model.addAttribute("searchCategories", lostItemService.getSearchCategoryNames());
    return "index";
  }
}
