package com.example.dogo.controller.area;

import com.example.dogo.repository.area.KorailLostFoundCenterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
        
        List<Map<String, Object>> regionCenters = lostFoundCenterRepository.findAllWithCoordinates().stream()
            .filter(matchesRegion(region))
            .toList();

        return regionCenters.stream()
            .filter(matchesSubRegion(subRegion))
            .filter(matchesNeighborhood(neighborhood))
            .toList();
    }

    private Predicate<Map<String, Object>> matchesRegion(String region) {
        if (region == null || region.isBlank()) return c -> true;
        String normalizedRegion = region.length() >= 2 ? region.substring(0, 2) : region;
        String areaCode = getAreaCodeByRegion(normalizedRegion);

        return c -> containsAny((String) c.get("locationDetails"), region, normalizedRegion)
            || containsAny((String) c.get("stationName"), region, normalizedRegion)
            || containsAny((String) c.get("subRegion"), region, normalizedRegion)
            || telMatchesAreaCode((String) c.get("telNo"), areaCode);
    }

    private Predicate<Map<String, Object>> matchesSubRegion(String subRegion) {
        if (subRegion == null || subRegion.isBlank()) return c -> true;
        String normSub = subRegion.length() >= 2 ? subRegion.substring(0, 2) : subRegion;

        return c -> containsAny((String) c.get("subRegion"), subRegion, normSub)
            || containsAny((String) c.get("locationDetails"), subRegion, normSub)
            || containsAny((String) c.get("stationName"), subRegion, normSub);
    }

    private Predicate<Map<String, Object>> matchesNeighborhood(String neighborhood) {
        if (neighborhood == null || neighborhood.isBlank()) return c -> true;
        String normNeighborhood = neighborhood.length() >= 2 ? neighborhood.substring(0, 2) : neighborhood;

        return c -> containsAny((String) c.get("locationDetails"), neighborhood, normNeighborhood)
            || containsAny((String) c.get("stationName"), neighborhood, normNeighborhood);
    }

    private boolean containsAny(String value, String... candidates) {
        if (value == null || value.isBlank()) return false;
        String trimmed = value.trim();
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && trimmed.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean telMatchesAreaCode(String tel, String areaCode) {
        if (tel == null || tel.isBlank() || areaCode == null || areaCode.isBlank()) return false;
        String cleanTel = tel.replaceAll("[^0-9]", "");
        return cleanTel.startsWith(areaCode) || tel.contains("(" + areaCode + ")") || tel.contains(areaCode + ")");
    }

    private String getAreaCodeByRegion(String region) {
        if (region == null) return null;
        String r = (region.length() >= 2) ? region.substring(0, 2) : region;
        return switch (r) {
            case "서울" -> "02";
            case "부산" -> "051";
            case "대구" -> "053";
            case "인천" -> "032";
            case "광주" -> "062";
            case "대전" -> "042";
            case "울산" -> "052";
            case "세종" -> "044";
            case "경기" -> "031";
            case "강원" -> "033";
            case "충북" -> "043";
            case "충남" -> "041";
            case "전북" -> "063";
            case "전남" -> "061";
            case "경북" -> "054";
            case "경남" -> "055";
            case "제주" -> "064";
            default -> null;
        };
    }

}
