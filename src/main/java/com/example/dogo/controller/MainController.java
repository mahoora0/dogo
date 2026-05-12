package com.example.dogo.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("currentUri", "/"); // 메인 페이지 표시
    return "index";
  }
}
