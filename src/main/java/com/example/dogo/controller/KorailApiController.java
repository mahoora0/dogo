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
    public List<Map<String, Object>> getLostFoundCenters(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String region,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String subRegion,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String neighborhood) {
        
        log.info("API CALL: getLostFoundCenters - Region: {}, SubRegion: {}, Neighborhood: {}", region, subRegion, neighborhood);
        List<Map<String, Object>> allCenters = lostFoundCenterRepository.findAllWithCoordinates();
        
        return allCenters.stream()
            .filter(c -> {
                if (region == null || region.isBlank()) return true;
                
                String name = (String) c.get("stationName");
                String operator = (String) c.get("operatorName");
                String details = (String) c.get("locationDetails");
                
                // Precise match for region to avoid false positives (e.g., "부산미도어묵" matching "부산")
                boolean regionMatch = (name != null && name.contains(region)) || 
                                     (operator != null && operator.contains(region)) ||
                                     (details != null && (
                                         details.contains(region + " ") || 
                                         details.contains(region + "시") || 
                                         details.contains(region + "광역시") || 
                                         details.contains(region + "도") ||
                                         details.startsWith(region)
                                     ));
                return regionMatch;
            })
            .filter(c -> {
                if (subRegion == null || subRegion.isBlank()) return true;
                String details = (String) c.get("locationDetails");
                String name = (String) c.get("stationName");
                return (details != null && details.contains(subRegion)) || 
                       (name != null && name.contains(subRegion));
            })
            .filter(c -> {
                if (neighborhood == null || neighborhood.isBlank()) return true;
                String details = (String) c.get("locationDetails");
                return (details != null && details.contains(neighborhood));
            })
            .collect(java.util.stream.Collectors.toList());
    }
}
