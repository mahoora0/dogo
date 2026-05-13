package com.example.dogo.config;

import com.example.dogo.entity.Area;
import com.example.dogo.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AreaDataInitializer implements CommandLineRunner {

    private final AreaRepository areaRepository;

    @Override
    public void run(String... args) throws Exception {
        if (areaRepository.count() > 0) {
            log.info("Area data already exists. Skipping initialization.");
            return;
        }

        log.info("Initializing area data...");

        List<Area> areas = Arrays.asList(
            createArea("서울", "SEOUL", 37.5665, 126.9780, 8, 1),
            createArea("부산", "BUSAN", 35.1796, 129.0756, 8, 2),
            createArea("대구", "DAEGU", 35.8714, 128.6014, 8, 3),
            createArea("인천", "INCHEON", 37.4563, 126.7052, 8, 4),
            createArea("광주", "GWANGJU", 35.1595, 126.8526, 8, 5),
            createArea("대전", "DAEJEON", 36.3504, 127.3845, 8, 6),
            createArea("울산", "ULSAN", 35.5384, 129.3114, 8, 7),
            createArea("세종", "SEJONG", 36.4800, 127.2890, 8, 8),
            createArea("경기", "GYEONGGI", 37.4138, 127.5183, 10, 9),
            createArea("강원", "GANGWON", 37.8228, 128.1555, 10, 10),
            createArea("충북", "CHUNGBUK", 36.6357, 127.4913, 10, 11),
            createArea("충남", "CHUNGNAM", 36.6588, 126.6728, 10, 12),
            createArea("전북", "JEONBUK", 35.8204, 127.1087, 10, 13),
            createArea("전남", "JEONNAM", 34.8679, 126.9910, 10, 14),
            createArea("경북", "GYEONGBUK", 36.5760, 128.5058, 10, 15),
            createArea("경남", "GYEONGNAM", 35.2377, 128.6924, 10, 16),
            createArea("제주", "JEJU", 33.4890, 126.4983, 9, 17)
        );

        areaRepository.saveAll(areas);
        log.info("Successfully initialized {} areas.", areas.size());
    }

    private Area createArea(String name, String code, Double lat, Double lng, Integer level, int order) {
        return Area.builder()
            .areaName(name)
            .areaCode(code)
            .latitude(lat)
            .longitude(lng)
            .defaultLevel(level)
            .sortOrder(order)
            .isActive(true)
            .build();
    }
}
