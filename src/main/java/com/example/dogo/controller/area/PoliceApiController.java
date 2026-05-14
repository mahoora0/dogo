package com.example.dogo.controller.area;

import com.example.dogo.entity.area.PoliceStation;
import com.example.dogo.repository.area.PoliceStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/police")
public class PoliceApiController {

  private final PoliceStationRepository policeStationRepository;

  @GetMapping
  public List<PoliceStation> getPoliceByRegion(
      @RequestParam String region,
      @RequestParam(required = false) String subRegion,
      @RequestParam(required = false) String neighborhood) {
    String pnuPrefix = getPnuPrefixByRegion(region);
    if (pnuPrefix != null) {
      List<PoliceStation> stations = policeStationRepository.findByPnuStartingWith(pnuPrefix);
      return stations.stream()
          .filter(s -> {
              if (subRegion == null || subRegion.isBlank()) return true;
              String rgn = s.getCmptncRgnNm();
              String addr = s.getAddr();
              String address = s.getAddress();
              String address1 = s.getAddress1();
              
              // Check all relevant fields for subRegion match
              return (rgn != null && (rgn.contains(subRegion) || subRegion.contains(rgn))) || 
                     (addr != null && addr.contains(subRegion)) ||
                     (address != null && address.contains(subRegion)) ||
                     (address1 != null && address1.contains(subRegion));
          })
          .filter(s -> {
              if (neighborhood == null || neighborhood.isBlank()) return true;
              // Check all relevant fields for neighborhood match
              return (s.getAddr() != null && s.getAddr().contains(neighborhood)) ||
                     (s.getAddress() != null && s.getAddress().contains(neighborhood)) ||
                     (s.getAddress1() != null && s.getAddress1().contains(neighborhood));
          })
          .toList();
    }
    return policeStationRepository.findByAddrContainingOrCmptncRgnNmContaining(region, region);
  }

  @GetMapping("/sub-regions")
  public List<String> getSubRegions(@RequestParam String region) {
    long totalCount = policeStationRepository.count();
    String pnuPrefix = getPnuPrefixByRegion(region);

    
    if (pnuPrefix != null) {
      List<PoliceStation> stations = policeStationRepository.findByPnuStartingWith(pnuPrefix);

      
      List<String> subRegions = stations.stream()
          .map(s -> {
            // Priority 1: Extract from address (usually has the most accurate 'Gu/Gun/Si' name)
            String rgn = extractDistrict(s.getAddr());
            
            // Priority 2: Use jurisdiction name from DB
            if (rgn == null || rgn.isBlank()) {
                rgn = s.getCmptncRgnNm();
            }
            
            if (rgn == null || rgn.isBlank()) return null;

            // Strip region name prefix if it exists (e.g., "서울강남구" -> "강남구")
            // But be careful not to strip "서울" from "서울특별시" in a way that leaves "특별시"
            if (region != null && rgn.startsWith(region)) {
                String remainder = rgn.substring(region.length()).trim();
                if (!remainder.isEmpty() && !remainder.startsWith("특별시") && !remainder.startsWith("광역시")) {
                    return remainder;
                }
            }
            
            // If it's something like "서울특별시 강남구", and region is "서울", 
            // we might want just "강남구".
            if (rgn.contains(" ")) {
                String[] parts = rgn.split("\\s+");
                if (parts.length >= 2 && (parts[1].endsWith("구") || parts[1].endsWith("군") || parts[1].endsWith("시"))) {
                    return parts[1];
                }
            }

            return rgn;
          })
          .filter(n -> n != null && !n.isBlank() && n.length() > 1)
          .distinct()
          .sorted()
          .toList();
          

      return subRegions;
    }
    return List.of();
  }

  private String extractDistrict(String addr) {
    if (addr == null) return null;
    String[] parts = addr.split("\\s+");
    // Usually the second part of the address is the district (Gu/Gun/Si)
    if (parts.length >= 2) {
      String district = parts[1];
      if (district.endsWith("구") || district.endsWith("군") || district.endsWith("시")) {
        return district;
      }
    }
    // If the first part already contains the district (e.g. "세종특별자치시")
    if (parts.length >= 1) {
        String first = parts[0];
        if (first.endsWith("구") || first.endsWith("군")) {
            return first;
        }
    }
    return null;
  }

  @GetMapping("/neighborhoods")
  public List<String> getNeighborhoods(@RequestParam String region, @RequestParam String subRegion) {
    String pnuPrefix = getPnuPrefixByRegion(region);

    
    if (pnuPrefix != null) {
      List<PoliceStation> stations = policeStationRepository.findByPnuStartingWith(pnuPrefix);
      List<String> neighborhoods = stations.stream()
          .filter(s -> {
              String rgn = s.getCmptncRgnNm();
              String addr = s.getAddr();
              // subRegion이 rgn에 포함되거나 rgn이 subRegion에 포함되거나 주소에 subRegion이 있는 경우
              return (rgn != null && (rgn.contains(subRegion) || subRegion.contains(rgn))) || 
                     (addr != null && addr.contains(subRegion));
          })
          .map(s -> {
              // address1(지번주소)에서 3번째 단어 시도
              String n = extractFromAddr(s.getAddress1(), 2); // 0-based index 2 is 3rd word
              if (n != null) return n;
              
              // address(도로명주소)의 괄호 안 시도
              n = extractFromParentheses(s.getAddress());
              if (n != null) return n;
              
              // 마지막 수단으로 addr 시도
              return extractFromAddr(s.getAddr(), 2);
          })
          .filter(n -> n != null && !n.isBlank())
          .distinct()
          .sorted()
          .toList();
      

      return neighborhoods;
    }
    return List.of();
  }

  private String extractFromAddr(String addr, int wordIndex) {
    if (addr == null) return null;
    String[] parts = addr.split("\\s+");
    if (parts.length > wordIndex) {
        String part = parts[wordIndex];
        if (part.endsWith("동") || part.endsWith("읍") || part.endsWith("면") || part.matches(".*[동읍면][0-9]*가?$")) {
            return part.replaceAll("[0-9-]+$", "");
        }
    }
    // 인덱스가 안 맞으면 전체 순회 시도
    for (String part : parts) {
        if (part.endsWith("동") || part.endsWith("읍") || part.endsWith("면") || part.matches(".*[동읍면][0-9]*가?$")) {
            return part.replaceAll("[0-9-]+$", "");
        }
    }
    return null;
  }

  private String extractFromParentheses(String addr) {
    if (addr == null) return null;
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^)]*[동읍면])[0-9]*[가]?\\)").matcher(addr);
    if (m.find()) {
        return m.group(1);
    }
    return null;
  }

  private String getPnuPrefixByRegion(String region) {
    if (region == null) return null;
    return switch (region) {
      case "서울" -> "11";
      case "부산" -> "26";
      case "대구" -> "27";
      case "인천" -> "28";
      case "광주" -> "29";
      case "대전" -> "30";
      case "울산" -> "31";
      case "세종" -> "36";
      case "경기" -> "41";
      case "강원" -> "51";
      case "충북" -> "43";
      case "충남" -> "44";
      case "전북" -> "52";
      case "전남" -> "46";
      case "경북" -> "47";
      case "경남" -> "48";
      case "제주" -> "50";
      default -> null;
    };
  }
}