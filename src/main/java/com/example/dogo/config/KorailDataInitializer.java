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
        try {
            java.io.InputStream is = getInputStream(fileName);
            if (is == null) return;
            
            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(is, Charset.forName("MS949")))
                    .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                    .build()) {
                
                reader.readNext(); // Skip header
                int count = 0;
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < 4) continue;
                    try {
                        String lName = clean(line[0]);
                        String sName = clean(line[1]);
                        if (sName.endsWith("역")) sName = sName.substring(0, sName.length() - 1);
                        
                        double lat = parseD(line[2]);
                        double lng = parseD(line[3]);
                        Integer exits = (line.length > 4) ? parseI(line[4]) : null;

                        jdbcTemplate.update(
                            "INSERT INTO korail_station_location (line_name, station_name, latitude, longitude, exit_count) VALUES (?, ?, ?, ?, ?)",
                            lName, sName, lat, lng, exits
                        );
                        count++;
                    } catch (Exception e) {
                        if (count < 1) log.error("Station Insert Error: {}", e.getMessage());
                    }
                }
                log.info("Successfully inserted {} stations via JDBC", count);
            }
        } catch (Exception e) {
            log.error("Station init failed", e);
        }
    }

    private void initializeCenters() {
        String fileName = "data/KORAIL_Lost_and_Found_Centers_.csv";
        try {
            java.io.InputStream is = getInputStream(fileName);
            if (is == null) return;

            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(is, Charset.forName("MS949")))
                    .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                    .build()) {
                
                reader.readNext(); // Skip header
                int count = 0;
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < 3) continue;
                    try {
                        String sName = clean(line[2]);
                        if (sName.endsWith("역")) sName = sName.substring(0, sName.length() - 1);

                        jdbcTemplate.update(
                            "INSERT INTO korail_lost_found_center (operator_name, line_name, station_name, location_details, operating_hours, tel_no) VALUES (?, ?, ?, ?, ?, ?)",
                            clean(line[0]), clean(line[1]), sName, 
                            line.length > 6 ? clean(line[6]) : null,
                            line.length > 7 ? clean(line[7]) : null,
                            line.length > 8 ? clean(line[8]) : null
                        );
                        count++;
                    } catch (Exception e) {
                        if (count < 1) log.error("Center Insert Error: {}", e.getMessage());
                    }
                }
                log.info("Successfully inserted {} centers via JDBC", count);
            }
        } catch (Exception e) {
            log.error("Center init failed", e);
        }
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
