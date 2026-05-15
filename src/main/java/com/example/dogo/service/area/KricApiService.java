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

    List<String> stationNames = List.of("서울", "부산", "대전", "동대구");

    for (String stationName : stationNames) {
      try {
        loadSingleStation(stationName);
      } catch (Exception e) {
        System.out.println("실패: " + stationName);
      }
    }
  }

  private void loadSingleStation(String stationName) {

    String encodedName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);

    String stationUrl =
        "https://openapi.kric.go.kr/openapi/convenientInfo/stationInfo"
            + "?serviceKey=" + apiKey
            + "&format=json"
            + "&railOprIsttCd=KR"
            + "&lnCd=1"
            + "&stinNm=" + encodedName;

    String stationResponse = restTemplate.getForObject(stationUrl, String.class);

    JSONObject stationJson = new JSONObject(stationResponse);
    JSONArray stationArray = stationJson
        .getJSONObject("response")
        .getJSONObject("body")
        .getJSONObject("items")
        .getJSONArray("item");

    if (stationArray.isEmpty()) return;

    JSONObject station = stationArray.getJSONObject(0);

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

    String lostResponse = restTemplate.getForObject(lostUrl, String.class);

    JSONObject lostJson = new JSONObject(lostResponse);
    JSONArray lostArray = lostJson
        .getJSONObject("response")
        .getJSONObject("body")
        .getJSONObject("items")
        .getJSONArray("item");

    if (lostArray.isEmpty()) return;

    JSONObject lost = lostArray.getJSONObject(0);

    SubwayLostCenter center = SubwayLostCenter.builder()
        .operatorName(lost.optString("railOprIsttNm"))
        .lineName(lost.optString("lnNm"))
        .region(stationName) // Using stationName as region (서울, 부산 등)
        .stationName(stationName)
        .latitude(lat)
        .longitude(lng)
        .detailLocation(lost.optString("dtlLoc"))
        .availableTime(lost.optString("utlPsbHr"))
        .telNo(lost.optString("telNo"))
        .build();

    repository.save(center);
  }

  public List<SubwayLostCenterDTO> getSubwayList(String region, String subRegion, String neighborhood) {
    List<SubwayLostCenter> regionCenters = repository.findAll().stream()
        .filter(matchesRegion(region))
        .toList();

    return regionCenters.stream()
        .filter(matchesSubRegion(subRegion))
        .filter(matchesNeighborhood(neighborhood))
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
