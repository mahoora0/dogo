package com.example.dogo.config;

import com.example.dogo.entity.area.PoliceStation;
import com.example.dogo.repository.area.PoliceStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

@Component
@RequiredArgsConstructor
public class PoliceDataLoader implements CommandLineRunner {

  private final PoliceStationRepository repository;

  @Override
  public void run(String... args) throws Exception {

    if (repository.count() > 0) return;

    BufferedReader br = new BufferedReader(
        new InputStreamReader(
            new FileInputStream("C:/workspace/dogo/src/main/resources/data/police_station.csv"),
            "UTF-8"
        )
    );

    String line;
    br.readLine();

    while ((line = br.readLine()) != null) {
      String[] data = line.split(",");

      if (data.length < 14) continue;

      PoliceStation station = new PoliceStation();

      station.setPnu(data[0].trim());                 // PNU
      station.setLongitude(Double.parseDouble(data[3].trim())); // LONGITUDE
      station.setLatitude(Double.parseDouble(data[4].trim()));  // LATITUDE
      station.setAddress(data[5].trim());             // ADDRESS
      station.setAddress1(data[6].trim());            // ADDRESS1
      station.setNoValue(Integer.parseInt(data[7].trim())); // NO
      station.setLclsf(data[8].trim());               // LCLSF
      station.setCmptncRgnNm(data[9].trim());         // CMPTNC_RGN_NM
      station.setPolstnNm(data[10].trim());           // POLSTN_NM
      station.setSe(data[11].trim());                 // SE
      station.setTelno(data[12].trim());              // TELNO
      station.setAddr(data[13].trim());               // ADDR

      repository.save(station);
    }

    br.close();
    System.out.println("경찰관서 데이터 저장 완료");
  }
}