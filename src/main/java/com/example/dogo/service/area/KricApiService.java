package com.example.dogo.service.area;

import com.example.dogo.dto.area.SubwayLostCenterDTO;
import com.example.dogo.entity.area.SubwayLostCenter;
import com.example.dogo.repository.area.SubwayLostCenterRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class KricApiService {

  private final SubwayLostCenterRepository repository;
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${kric.api.key}")
  private String apiKey;

  public void loadSubwayData() {
    repository.deleteAll();

    List<String> stationNames = List.of(
        "서울", "용산", "영등포", "청량리", "수서", "시청", "강남", "잠실", "홍대입구",
        "부산", "서면", "해운대", "남포",
        "동대구", "대구", "반월당",
        "대전", "서대전",
        "광주", "광주송정", "상무",
        "인천", "부평", "인천시청",
        "수원", "안양", "부천", "의정부", "일산", "판교"
    );

    for (String stationName : stationNames) {
      try {
        loadSingleStation(stationName);
      } catch (Exception e) {
        // Skip failures
      }
    }
  }

  private void loadSingleStation(String stationName) {

    String encodedName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);

    String stationUrl =
        "https://openapi.kric.go.kr/openapi/convenientInfo/stationInfo"
            + "?serviceKey=" + apiKey
            + "&format=json"
            + "&stinNm=" + encodedName;

    String stationResponse = restTemplate.getForObject(stationUrl, String.class);

    JSONObject stationJson = new JSONObject(stationResponse);
    if (!stationJson.has("response") || stationJson.getJSONObject("response").isNull("body")) return;
    
    JSONObject body = stationJson.getJSONObject("response").getJSONObject("body");
    if (!body.has("items") || body.isNull("items")) return;
    
    JSONArray stationArray = body.getJSONObject("items").getJSONArray("item");

    if (stationArray.isEmpty()) return;

    for (int i = 0; i < Math.min(stationArray.length(), 5); i++) {
        JSONObject station = stationArray.getJSONObject(i);

        String railCode = station.optString("railOprIsttCd");
        String lineCode = station.optString("lnCd");
        String stationCode = station.optString("stinCd");

        Double lat = station.optDouble("stinLocLat");
        Double lng = station.optDouble("stinLocLon");

        String lostUrl =
            "https://openapi.kric.go.kr/openapi/convenientInfo/stationLostPropertyOffice"
                + "?serviceKey=" + apiKey
                + "&format=json"
                + "&railOprIsttCd=" + railCode
                + "&lnCd=" + lineCode
                + "&stinCd=" + stationCode;

        try {
            String lostResponse = restTemplate.getForObject(lostUrl, String.class);
            JSONObject lostJson = new JSONObject(lostResponse);
            if (!lostJson.has("response") || lostJson.getJSONObject("response").isNull("body")) continue;
            
            JSONObject lostBody = lostJson.getJSONObject("response").getJSONObject("body");
            if (!lostBody.has("items") || lostBody.isNull("items")) continue;
            
            JSONArray lostArray = lostBody.getJSONObject("items").getJSONArray("item");

            if (lostArray.isEmpty()) continue;

            JSONObject lost = lostArray.getJSONObject(0);
            String operatorName = lost.optString("railOprIsttNm");
            String telNo = lost.optString("telNo");
            String region = determineRegion(operatorName, telNo, stationName);

            SubwayLostCenter center = SubwayLostCenter.builder()
                .operatorName(operatorName)
                .lineName(lost.optString("lnNm"))
                .region(region)
                .stationName(stationName)
                .latitude(lat)
                .longitude(lng)
                .detailLocation(lost.optString("dtlLoc"))
                .availableTime(lost.optString("utlPsbHr"))
                .telNo(telNo)
                .build();

            repository.save(center);
        } catch (Exception e) {
            // Ignore individual failures
        }
    }
  }

  private String determineRegion(String operatorName, String telNo, String stationName) {
      if (operatorName.contains("서울") || (telNo != null && telNo.startsWith("02"))) return "서울";
      if (operatorName.contains("부산") || (telNo != null && telNo.startsWith("051"))) return "부산";
      if (operatorName.contains("대구") || (telNo != null && telNo.startsWith("053"))) return "대구";
      if (operatorName.contains("인천") || (telNo != null && telNo.startsWith("032"))) return "인천";
      if (operatorName.contains("대전") || (telNo != null && telNo.startsWith("042"))) return "대전";
      if (operatorName.contains("광주") || (telNo != null && telNo.startsWith("062"))) return "광주";
      
      if (telNo != null) {
          if (telNo.startsWith("031")) return "경기";
          if (telNo.startsWith("033")) return "강원";
          if (telNo.startsWith("041")) return "충남";
          if (telNo.startsWith("043")) return "충북";
          if (telNo.startsWith("054")) return "경북";
          if (telNo.startsWith("055")) return "경남";
          if (telNo.startsWith("061")) return "전남";
          if (telNo.startsWith("063")) return "전북";
      }
      return stationName; 
  }

  public List<SubwayLostCenterDTO> getSubwayList(String region, String subRegion, String neighborhood, String keyword) {
    if (region == null || region.isBlank()) {
      if (keyword == null || keyword.isBlank()) {
        return List.of();
      }
      return repository.findAll().stream()
          .filter(entity -> {
              String name = entity.getStationName() != null ? entity.getStationName() : "";
              String detail = entity.getDetailLocation() != null ? entity.getDetailLocation() : "";
              return name.contains(keyword) || detail.contains(keyword);
          })
          .map(entity -> SubwayLostCenterDTO.builder()
              .id(entity.getId())
              .operatorName(entity.getOperatorName())
              .lineName(entity.getLineName())
              .region(entity.getRegion())
              .subRegion(entity.getSubRegion())
              .stationName(entity.getStationName())
              .latitude(entity.getLatitude())
              .longitude(entity.getLongitude())
              .detailLocation(entity.getDetailLocation())
              .availableTime(entity.getAvailableTime())
              .telNo(entity.getTelNo())
              .build())
          .toList();
    }

    List<SubwayLostCenter> regionCenters = repository.findAll().stream()
        .filter(matchesRegion(region))
        .toList();

    return regionCenters.stream()
        .filter(matchesSubRegion(subRegion))
        .map(entity -> SubwayLostCenterDTO.builder()
            .id(entity.getId())
            .operatorName(entity.getOperatorName())
            .lineName(entity.getLineName())
            .region(entity.getRegion())
            .subRegion(entity.getSubRegion())
            .stationName(entity.getStationName())
            .latitude(entity.getLatitude())
            .longitude(entity.getLongitude())
            .detailLocation(entity.getDetailLocation())
            .availableTime(entity.getAvailableTime())
            .telNo(entity.getTelNo())
            .build())
        .toList();
  }

  private Predicate<SubwayLostCenter> matchesRegion(String region) {
    if (region == null || region.isBlank()) return s -> true;
    String normalizedRegion = region.length() >= 2 ? region.substring(0, 2) : region;
    String areaCode = getAreaCodeByRegion(normalizedRegion);
    return s -> startsWithRegion(s.getRegion(), region, normalizedRegion)
        || startsWithRegion(s.getSubRegion(), region, normalizedRegion)
        || containsAny(s.getStationName(), region, normalizedRegion)
        || telMatchesAreaCode(s.getTelNo(), areaCode);
  }

  private Predicate<SubwayLostCenter> matchesSubRegion(String subRegion) {
    if (subRegion == null || subRegion.isBlank()) return s -> true;
    String normSub = subRegion.length() >= 2 ? subRegion.substring(0, 2) : subRegion;
    return s -> containsAny(s.getSubRegion(), subRegion, normSub)
        || containsAny(s.getDetailLocation(), subRegion, normSub)
        || containsAny(s.getStationName(), subRegion, normSub);
  }

  private Predicate<SubwayLostCenter> matchesNeighborhood(String neighborhood) {
    if (neighborhood == null || neighborhood.isBlank()) return s -> true;
    String normNeighborhood = neighborhood.length() >= 2 ? neighborhood.substring(0, 2) : neighborhood;
    return s -> containsAny(s.getDetailLocation(), neighborhood, normNeighborhood)
        || containsAny(s.getStationName(), neighborhood, normNeighborhood);
  }

  private boolean containsAny(String value, String... candidates) {
    if (value == null || value.isBlank()) return false;
    for (String candidate : candidates) {
      if (candidate != null && !candidate.isBlank() && value.contains(candidate)) {
        return true;
      }
    }
    return false;
  }

  private boolean startsWithRegion(String value, String region, String normalizedRegion) {
    if (value == null || value.isBlank()) return false;
    String trimmed = value.trim();
    return trimmed.equals(region)
        || trimmed.equals(normalizedRegion)
        || trimmed.startsWith(region)
        || trimmed.startsWith(normalizedRegion);
  }

  private boolean telMatchesAreaCode(String tel, String areaCode) {
    if (tel == null || tel.isBlank() || areaCode == null || areaCode.isBlank()) return false;
    return tel.replaceAll("[^0-9]", "").startsWith(areaCode);
  }

  private String getAreaCodeByRegion(String region) {
    if (region == null) return null;
    String r = region.length() >= 2 ? region.substring(0, 2) : region;
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
