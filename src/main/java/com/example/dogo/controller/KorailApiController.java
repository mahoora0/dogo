package com.example.dogo.controller;

import com.example.dogo.repository.KorailLostFoundCenterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/korail")
public class KorailApiController {

    private final KorailLostFoundCenterRepository lostFoundCenterRepository;

    @GetMapping("/lost-found")
    public List<Map<String, Object>> getLostFoundCenters(@org.springframework.web.bind.annotation.RequestParam(required = false) String region) {
        log.info("API CALL: getLostFoundCenters - Region: {}", region);
        List<Map<String, Object>> allCenters = lostFoundCenterRepository.findAllWithCoordinates();
        
        if (region == null || region.isBlank()) {
            return allCenters;
        }
        
        // 지역 이름(예: "서울", "부산")이 포함된 센터만 필터링
        return allCenters.stream()
            .filter(c -> {
                String name = (String) c.get("stationName");
                String operator = (String) c.get("operatorName");
                String details = (String) c.get("locationDetails");
                
                return (name != null && name.contains(region)) || 
                       (operator != null && operator.contains(region)) ||
                       (details != null && details.contains(region));
            })
            .collect(java.util.stream.Collectors.toList());
    }
}
