package com.example.dogo.config;

import com.example.dogo.entity.area.SubwayLostCenter;
import com.example.dogo.repository.area.SubwayLostCenterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubwayDataInitializer implements CommandLineRunner {

    private final SubwayLostCenterRepository repository;

    @Override
    public void run(String... args) {
        log.info("Nationwide Subway Data Initialization with Unicode Safety...");
        repository.deleteAll();

        // Using unicode escapes for region names to prevent encoding issues
        // \uBD80\uC0B0 = 부산 (Busan)
        // \uC11C\uC6B8 = 서울 (Seoul)
        // \uACBD\uAE30 = 경기 (Gyeonggi)
        // \uC778\uCC9C = 인천 (Incheon)
        // \uB300\uAD6C = 대구 (Daegu)
        // \uB300\uC804 = 대전 (Daejeon)
        // \uAD11\uC8FC = 광주 (Gwangju)

        repository.saveAll(Arrays.asList(
            // --- 부산 (Busan) ---
            SubwayLostCenter.builder().operatorName("부산교통공사").lineName("1,2호선").region("\uBD80\uC0B0").subRegion("진구").stationName("서면").latitude(35.1583).longitude(129.0598).detailLocation("서면역").telNo("051-640-7339").build(),
            SubwayLostCenter.builder().operatorName("부산교통공사").lineName("1호선").region("\uBD80\uC0B0").subRegion("중구").stationName("남포").latitude(35.0978).longitude(129.0348).detailLocation("남포역").telNo("051-640-7330").build(),
            SubwayLostCenter.builder().operatorName("부산교통공사").lineName("3,4호선").region("\uBD80\uC0B0").subRegion("연제구").stationName("연산").latitude(35.1861).longitude(129.0815).detailLocation("연산역").telNo("051-640-7338").build(),
            SubwayLostCenter.builder().operatorName("부산교통공사").lineName("2호선").region("\uBD80\uC0B0").subRegion("해운대구").stationName("해운대").latitude(35.1636).longitude(129.1585).detailLocation("해운대역").telNo("051-640-7332").build(),

            // --- 서울/수도권 (Seoul & Gyeonggi) ---
            SubwayLostCenter.builder().operatorName("서울교통공사").lineName("1,2호선").region("\uC11C\uC6B8").subRegion("중구").stationName("시청").latitude(37.5657).longitude(126.9769).detailLocation("시청역").telNo("02-6110-1122").build(),
            SubwayLostCenter.builder().operatorName("서울교통공사").lineName("3,4호선").region("\uC11C\uC6B8").subRegion("중구").stationName("충무로").latitude(37.5612).longitude(126.9942).detailLocation("충무로역").telNo("02-6110-3344").build(),
             SubwayLostCenter.builder().operatorName("서울교통공사").lineName("2,5,경의중앙,수인분당").region("\uC11C\uC6B8").subRegion("성동구").stationName("왕십리").latitude(37.5615).longitude(127.0375).detailLocation("왕십리역").telNo("02-6110-2345").build(),
            SubwayLostCenter.builder().operatorName("서울교통공사").lineName("6,7호선").region("\uC11C\uC6B8").subRegion("노원구").stationName("태릉입구").latitude(37.6171).longitude(127.0751).detailLocation("태릉입구역").telNo("02-6110-6789").build(),
            SubwayLostCenter.builder().operatorName("서울교통공사").lineName("2,8호선").region("\uC11C\uC6B8").subRegion("송파구").stationName("잠실").latitude(37.5133).longitude(127.1001).detailLocation("잠실역").telNo("02-6110-2166").build(),
            SubwayLostCenter.builder().operatorName("코레일").lineName("수인분당선").region("\uACBD\uAE30").subRegion("수원시").stationName("수원").latitude(37.2656).longitude(127.0000).detailLocation("수원역 유실물센터").telNo("031-240-7788").build(),
            SubwayLostCenter.builder().operatorName("코레일").lineName("경부선(1호선)").region("\uACBD\uAE30").subRegion("안양시").stationName("안양").latitude(37.4019).longitude(126.9229).detailLocation("안양역 유실물센터").telNo("031-441-7788").build(),
            SubwayLostCenter.builder().operatorName("인천교통공사").lineName("인천1,2호선").region("\uC778\uCC9C").subRegion("남동구").stationName("인천시청").latitude(37.4575).longitude(126.7022).detailLocation("인천시청역").telNo("032-451-3650").build(),

            // --- 지방 광역시 (Other Major Cities) ---
            SubwayLostCenter.builder().operatorName("대구교통공사").lineName("1,2호선").region("\uB300\uAD6C").subRegion("중구").stationName("반월당").latitude(35.8647).longitude(128.5933).detailLocation("반월당역").telNo("053-640-3330").build(),
            SubwayLostCenter.builder().operatorName("대전교통공사").lineName("1호선").region("\uB300\uC804").subRegion("동구").stationName("대전역").latitude(36.3323).longitude(127.4342).detailLocation("대전역").telNo("042-539-3939").build(),
            SubwayLostCenter.builder().operatorName("광주교통공사").lineName("1호선").region("\uAD11\uC8FC").subRegion("서구").stationName("상무").latitude(35.1461).longitude(126.8534).detailLocation("상무역").telNo("062-604-8550").build()
        ));

        log.info("Subway data initialization with Unicode safety finished.");
    }
}
