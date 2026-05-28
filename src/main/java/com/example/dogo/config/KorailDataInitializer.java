package com.example.dogo.config;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.Charset;

@Component
@Slf4j
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class KorailDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info(">>>> KORAIL INITIALIZER START (JDBC MODE) <<<<");
            
            // 기존 데이터 삭제
            jdbcTemplate.execute("DELETE FROM korail_station_location");
            jdbcTemplate.execute("DELETE FROM korail_lost_found_center");

            initializeStations();
            initializeCenters();
            
            log.info(">>>> KORAIL INITIALIZER END <<<<");
        } catch (Exception e) {
            log.error("Fatal Error", e);
        }
    }

    private void initializeStations() {
        String fileName = "data/KORAIL_Station_Locations.csv";
        String[] encodings = {"MS949", "UTF-8", "EUC-KR"};
        
        for (String encoding : encodings) {
            try {
                java.io.InputStream is = getInputStream(fileName);
                if (is == null) continue;
                
                try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(is, Charset.forName(encoding)))
                        .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                        .build()) {
                    
                    String[] header = reader.readNext();
                    if (header == null) continue;
                    
                    String headerStr = String.join("", header);
                    // Improved check: If UTF-8, it shouldn't contain garbage characters common when reading MS949 as UTF-8
                    if (encoding.equals("UTF-8") && (headerStr.contains("\uFFFD") || headerStr.matches(".*[-].*"))) {
                        continue;
                    }

                    int count = 0;
                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        if (line.length < 4) continue;
                        String lName = clean(line[0]);
                        String sName = clean(line[1]);
                        if (sName.endsWith("역")) sName = sName.substring(0, sName.length() - 1);
                        double lat = parseD(line[2]);
                        double lng = parseD(line[3]);
                        Integer exits = (line.length > 4) ? parseI(line[4]) : null;

                        if (lat != 0 && lng != 0) {
                            jdbcTemplate.update(
                                "INSERT INTO korail_station_location (line_name, station_name, latitude, longitude, exit_count) VALUES (?, ?, ?, ?, ?)",
                                lName, sName, lat, lng, exits
                            );
                            count++;
                        }
                    }
                    if (count > 0) {
                        log.info("Successfully inserted {} stations via JDBC using {}", count, encoding);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed with encoding {}: {}", encoding, e.getMessage());
            }
        }
    }

    private void initializeCenters() {
        String fileName = "data/KORAIL_Lost_and_Found_Centers_.csv";
        String[] encodings = {"MS949", "UTF-8", "EUC-KR"};
        
        for (String encoding : encodings) {
            try {
                java.io.InputStream is = getInputStream(fileName);
                if (is == null) continue;

                try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(is, Charset.forName(encoding)))
                        .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                        .build()) {
                    
                    String[] header = reader.readNext();
                    if (header == null) continue;
                    
                    String headerStr = String.join("", header);
                    if (encoding.equals("UTF-8") && (headerStr.contains("\uFFFD") || headerStr.matches(".*[-].*"))) {
                        continue;
                    }

                    int count = 0;
                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        if (line.length < 3) continue;
                        String opName = clean(line[0]);
                        String lName = clean(line[1]);
                        String sName = clean(line[2]);
                        if (sName.endsWith("역")) sName = sName.substring(0, sName.length() - 1);
                        String details = line.length > 6 ? clean(line[6]) : null;
                        String telNo = line.length > 8 ? clean(line[8]) : null;
                        String subRegion = extractSubRegion(details, sName, telNo);

                        jdbcTemplate.update(
                            "INSERT INTO korail_lost_found_center (operator_name, line_name, station_name, sub_region, location_details, operating_hours, tel_no) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            opName, lName, sName, subRegion, details,
                            line.length > 7 ? clean(line[7]) : null,
                            telNo
                        );
                        count++;
                    }
                    if (count > 0) {
                        log.info("Successfully inserted {} centers via JDBC using {}", count, encoding);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed with encoding {}: {}", encoding, e.getMessage());
            }
        }
    }


    private String extractSubRegion(String details, String stationName, String telNo) {
        // 1. Priority: Hardcoded mappings for known major stations
        if (stationName.contains("부산")) return "부산 동구";
        if (stationName.contains("부전")) return "부산 진구";
        if (stationName.contains("서울")) return "서울 중구";
        if (stationName.contains("용산")) return "서울 용산구";
        if (stationName.contains("영등포")) return "서울 영등포구";
        if (stationName.contains("청량리")) return "서울 동대문구";
        if (stationName.contains("수원")) return "경기 수원시";
        if (stationName.contains("안양")) return "경기 안양시";
        if (stationName.contains("부천")) return "경기 부천시";
        if (stationName.contains("역곡")) return "경기 부천시";
        if (stationName.contains("광명")) return "경기 광명시";
        if (stationName.contains("대전")) return "대전 동구";
        if (stationName.contains("천안")) return "충남 천안시";
        if (stationName.contains("아산")) return "충남 아산시";
        if (stationName.contains("광주송정")) return "광주 광산구";
        if (stationName.contains("동대구")) return "대구 동구";
        if (stationName.contains("울산")) return "울산 남구";
        if (stationName.contains("포항")) return "경북 포항시";
        if (stationName.contains("창원")) return "경남 창원시";
        if (stationName.contains("마산")) return "경남 마산회원구";
        if (stationName.contains("강릉")) return "강원 강릉시";
        if (stationName.contains("춘천")) return "강원 춘천시";
        if (stationName.contains("원주")) return "강원 원주시";
        if (stationName.contains("대구")) return "대구 동구";
        if (stationName.contains("인천")) return "인천 중구";

        // 2. Parse from location details physical address (Most reliable fallback)
        if (details != null && !details.isBlank()) {
            String province = extractProvinceFromAddress(details);
            String[] parts = details.split("\\s+");
            String localUnit = null;
            for (String part : parts) {
                if (part.endsWith("구")) {
                    localUnit = part;
                    break;
                }
                if (part.endsWith("시") && !part.equals(parts[0])) {
                    localUnit = part;
                }
                if (part.endsWith("군") && localUnit == null) {
                    localUnit = part;
                }
            }
            if (province != null) {
                if (localUnit != null) {
                    return province + " " + localUnit;
                }
                return province;
            }
            if (localUnit != null) {
                return localUnit;
            }
        }

        // 3. Use Tel No area code to determine region (only if not a 042 corporate phone number, since KORAIL uses 042 centrally)
        if (telNo != null && !telNo.isBlank()) {
            String cleanTel = telNo.replaceAll("[^0-9]", "");
            if (cleanTel.startsWith("02")) return "서울";
            if (cleanTel.startsWith("031")) return "경기";
            if (cleanTel.startsWith("032")) return "인천";
            if (cleanTel.startsWith("033")) return "강원";
            if (cleanTel.startsWith("041")) return "충남";
            // KORAIL corporate numbers start with 042, so we ignore 042 here to prevent false mapping to Daejeon
            if (cleanTel.startsWith("042") && !stationName.contains("대전")) {
                // Ignore!
            } else if (cleanTel.startsWith("042")) {
                return "대전";
            }
            if (cleanTel.startsWith("043")) return "충북";
            if (cleanTel.startsWith("044")) return "세종";
            if (cleanTel.startsWith("051")) return "부산";
            if (cleanTel.startsWith("052")) return "울산";
            if (cleanTel.startsWith("053")) return "대구";
            if (cleanTel.startsWith("054")) return "경북";
            if (cleanTel.startsWith("055")) return "경남";
            if (cleanTel.startsWith("061")) return "전남";
            if (cleanTel.startsWith("062")) return "광주";
            if (cleanTel.startsWith("063")) return "전북";
            if (cleanTel.startsWith("064")) return "제주";
        }

        return null;
    }

    private String extractProvinceFromAddress(String details) {
        if (details == null || details.isBlank()) return null;
        String cleanDetails = details.trim();
        if (cleanDetails.startsWith("서울")) return "서울";
        if (cleanDetails.startsWith("부산")) return "부산";
        if (cleanDetails.startsWith("대구")) return "대구";
        if (cleanDetails.startsWith("인천")) return "인천";
        if (cleanDetails.startsWith("광주")) return "광주";
        if (cleanDetails.startsWith("대전")) return "대전";
        if (cleanDetails.startsWith("울산")) return "울산";
        if (cleanDetails.startsWith("세종")) return "세종";
        if (cleanDetails.startsWith("경기")) return "경기";
        if (cleanDetails.startsWith("강원")) return "강원";
        if (cleanDetails.startsWith("충북") || cleanDetails.startsWith("충청북")) return "충북";
        if (cleanDetails.startsWith("충남") || cleanDetails.startsWith("충청남")) return "충남";
        if (cleanDetails.startsWith("전북") || cleanDetails.startsWith("전라북")) return "전북";
        if (cleanDetails.startsWith("전남") || cleanDetails.startsWith("전라남")) return "전남";
        if (cleanDetails.startsWith("경북") || cleanDetails.startsWith("경상북")) return "경북";
        if (cleanDetails.startsWith("경남") || cleanDetails.startsWith("경상남")) return "경남";
        if (cleanDetails.startsWith("제주")) return "제주";
        return null;
    }




    private java.io.InputStream getInputStream(String fileName) throws java.io.IOException {
        java.io.File file = new java.io.File("src/main/resources/" + fileName);
        if (file.exists()) return new java.io.FileInputStream(file);
        org.springframework.core.io.Resource resource = new ClassPathResource(fileName);
        return resource.exists() ? resource.getInputStream() : null;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("[\\uFEFF\\u200B\\u00A0]", "");
    }

    private double parseD(String s) {
        try { return Double.parseDouble(clean(s).replaceAll("[^0-9.]", "")); } catch (Exception e) { return 0.0; }
    }

    private Integer parseI(String s) {
        try { return Integer.parseInt(clean(s).replaceAll("[^0-9]", "")); } catch (Exception e) { return null; }
    }
}
