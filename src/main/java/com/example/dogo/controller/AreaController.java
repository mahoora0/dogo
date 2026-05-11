package com.example.dogo.controller;

import com.example.dogo.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AreaController {

  private final AreaService areaService;

  @GetMapping("/areas/list")
  public String areaList(Model model) {
    model.addAttribute("currentUri", "/areas/list");
    model.addAttribute("areas", areaService.getActiveAreas());
    return "area/list";
  }
}
