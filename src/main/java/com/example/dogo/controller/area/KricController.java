package com.example.dogo.controller.area;

import com.example.dogo.dto.area.SubwayLostCenterDTO;
import com.example.dogo.service.area.KricApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class KricController {

  private final KricApiService kricApiService;

  @GetMapping("/api/load-subway")
  public String loadSubway() {
    kricApiService.loadSubwayData();
    return "저장 완료";
  }

  @GetMapping("/api/subway/list")
  public List<SubwayLostCenterDTO> getSubwayList(
          @RequestParam(required = false) String region,
          @RequestParam(required = false) String subRegion,
          @RequestParam(required = false) String neighborhood,
          @RequestParam(required = false) String keyword) {
    return kricApiService.getSubwayList(region, subRegion, neighborhood, keyword);
  }
}