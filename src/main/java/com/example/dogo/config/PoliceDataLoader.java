package com.example.dogo.config;

import com.example.dogo.entity.area.PoliceStation;
import com.example.dogo.repository.area.PoliceStationRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoliceDataLoader implements CommandLineRunner {

  private final PoliceStationRepository repository;

  @Override
  public void run(String... args) throws Exception {

    if (repository.count() > 0) return;

    CSVReader reader = new CSVReader(
        new InputStreamReader(
            new ClassPathResource("data/police_station.csv").getInputStream(),
            "EUC-KR"
        )
    );

    String[] data;
    reader.readNext(); // 헤더 skip

    while ((data = reader.readNext()) != null) {

      if (data.length < 14) continue;

      PoliceStation station = new PoliceStation();

      station.setPnu(data[0].trim());
      station.setLongitude(Double.parseDouble(data[3].trim()));
      station.setLatitude(Double.parseDouble(data[4].trim()));
      station.setAddress(data[5].trim());
      station.setAddress1(data[6].trim());

      // 안전 처리
      try {
        station.setNoValue(Integer.parseInt(data[7].trim()));
      } catch (NumberFormatException e) {
        station.setNoValue(0);
      }

      station.setLclsf(data[8].trim());
      station.setCmptncRgnNm(data[9].trim());
      station.setPolstnNm(data[10].trim());
      station.setSe(data[11].trim());
      station.setTelno(data[12].trim());
      station.setAddr(data[13].trim());

      repository.save(station);
    }

    reader.close();
    log.info("경찰관서 데이터 저장 완료");
  }
}
