package com.example.dogo.controller;

import com.example.dogo.entity.PoliceStation;
import com.example.dogo.repository.PoliceStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/police")
public class PoliceApiController {

  private final PoliceStationRepository policeStationRepository;

  @GetMapping
  public List<PoliceStation> getPoliceByRegion(@RequestParam String region) {
    return policeStationRepository.findByAddrStartingWith(region);
  }
}