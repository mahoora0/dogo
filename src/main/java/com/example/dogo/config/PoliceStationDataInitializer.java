package com.example.dogo.config;

import com.example.dogo.entity.area.PoliceStation;
import com.example.dogo.repository.area.PoliceStationRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PoliceStationDataInitializer implements CommandLineRunner {

    private final PoliceStationRepository policeStationRepository;

    @Override
    public void run(String... args) throws Exception {
        // 이미 데이터가 있다면 삭제하고 재입력 (깨진 데이터 복구용)
        if (policeStationRepository.count() > 0) {
            log.info("Existing police station data found. Clearing for re-import with correct encoding...");
            policeStationRepository.deleteAll();
        }

        try {
            log.info("Starting police station data initialization...");
            java.io.File file = new java.io.File("src/main/resources/data/police_station.csv");
            java.io.InputStream is;
            
            // 시도할 인코딩 순서 (MS949 -> UTF-8)
            String[] encodings = {"MS949", "UTF-8", "EUC-KR"};
            boolean success = false;
            
            for (String enc : encodings) {
                if (success) break;
                
                log.info("Trying encoding: {}", enc);
                if (file.exists()) is = new java.io.FileInputStream(file);
                else is = new ClassPathResource("data/police_station.csv").getInputStream();
                
                try {
                    policeStationRepository.deleteAll();
                    readAndSaveStations(is, enc);
                    
                    // 읽어온 데이터가 깨졌는지 샘플 검사
                    PoliceStation sample = policeStationRepository.findAll().stream().findFirst().orElse(null);
                    if (sample != null && (sample.getPolstnNm().contains("\ufffd") || sample.getAddr().contains("\ufffd"))) {
                        throw new Exception("Detected broken characters with " + enc);
                    }
                    success = true;
                    log.info("Data initialization successful with {}", enc);
                } catch (Exception e) {
                    log.warn("{} failed or broken: {}", enc, e.getMessage());
                }
            }

            if (policeStationRepository.count() == 0) {
                log.warn("Database still empty. Inserting fallback sample record for Seoul.");
                PoliceStation sample = new PoliceStation();
                sample.setPnu("1114010300100310006");
                sample.setLatitude(37.5636);
                sample.setLongitude(126.9894);
                sample.setPolstnNm("서울중부경찰서");
                sample.setCmptncRgnNm("서울중부");
                sample.setAddr("서울특별시 중구 저동2가 62-1");
                sample.setTelno("02-330-9114");
                policeStationRepository.save(sample);
            }
            
            log.info("Initialization finished. Total stations: {}", policeStationRepository.count());
            
        } catch (Exception e) {
            log.error("Fatal error during initialization", e);
        }
    }

    private void readAndSaveStations(java.io.InputStream is, String encoding) throws Exception {
        try (CSVReader reader = new CSVReader(new java.io.InputStreamReader(is, Charset.forName(encoding)))) {
            String[] nextLine;
            // Skip header
            reader.readNext();
            
            List<PoliceStation> stations = new ArrayList<>();
            while (true) {
                try {
                    nextLine = reader.readNext();
                    if (nextLine == null) break;
                    
                    if (nextLine.length < 14) continue;
                    
                    PoliceStation station = new PoliceStation();
                    station.setPnu(nextLine[0]);
                    station.setLongitude(parseDouble(nextLine[3]));
                    station.setLatitude(parseDouble(nextLine[4]));
                    station.setAddress(nextLine[5]);
                    station.setAddress1(nextLine[6]);
                    station.setNoValue(parseInteger(nextLine[7]));
                    station.setLclsf(nextLine[8]);
                    station.setCmptncRgnNm(nextLine[9]);
                    station.setPolstnNm(nextLine[10]);
                    station.setSe(nextLine[11]);
                    station.setTelno(nextLine[12]);
                    station.setAddr(nextLine[13]);
                    
                    stations.add(station);
                    
                    if (stations.size() <= 5) {
                        log.info("Sample data ({}) - PNU: {}, Station: {}, Addr: {}", 
                            encoding, station.getPnu(), station.getPolstnNm(), station.getAddr());
                    }
                    
                    if (stations.size() >= 1000) {
                        policeStationRepository.saveAll(stations);
                        stations.clear();
                    }
                } catch (Exception ex) {
                    log.warn("Skipping a line due to error in encoding {}: {}", encoding, ex.getMessage());
                    // Just continue to next line
                }
            }
            
            if (!stations.isEmpty()) {
                policeStationRepository.saveAll(stations);
            }
            
            log.info("Finished import. Current database count: {}", policeStationRepository.count());
            
        } // End of try-with-resources
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
