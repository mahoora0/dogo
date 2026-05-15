package com.example.dogo.controller.area;

import com.example.dogo.dto.area.AreaDTO;
import com.example.dogo.service.area.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/areas")
public class AreaApiController {

  private final AreaService areaService;

  @GetMapping("/coords")
  public AreaDTO getCoords(@RequestParam String name) {
    return areaService.getAreaByName(name);
  }
}
