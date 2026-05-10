package com.example.dogo.controller;

import org.springframework.stereotype.Controller; // RestController가 아님!
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // HTML 템플릿을 찾기 위해 필수
public class AreaController {

  @GetMapping("/areas/list") // 브라우저 접속 주소
  public String areaList(Model model) {
    model.addAttribute("currentUri", "/areas/list");
    // 필요한 경우 서비스에서 데이터를 가져와 모델에 추가
    return "area/list"; // src/main/resources/templates/area/list.html 실행
  }
}