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
            SubwayLostCenter.builder().operatorName("서울교통공사").lineName("5호선").region("\uC11C\uC6B8").subRegion("강서구").stationName("까치산").latitude(37.5317).longitude(126.8467).detailLocation("까치산역").telNo("02-6110-5341").build(),
            SubwayLostCenter.builder().operatorName("서울교통공사").lineName("7호선").region("\uC11C\uC6B8").subRegion("광진구").stationName("건대입구").latitude(37.5404).longitude(127.0692).detailLocation("건대입구역").telNo("02-6110-7271").build(),
            
            SubwayLostCenter.builder().operatorName("코레일").lineName("수인분당선").region("\uACBD\uAE30").subRegion("수원시").stationName("수원").latitude(37.2656).longitude(127.0000).detailLocation("수원역 유실물센터").telNo("031-240-7788").build(),
            SubwayLostCenter.builder().operatorName("코레일").lineName("경부선(1호선)").region("\uACBD\uAE30").subRegion("안양시").stationName("안양").latitude(37.4019).longitude(126.9229).detailLocation("안양역 유실물센터").telNo("031-441-7788").build(),
            SubwayLostCenter.builder().operatorName("코레일").lineName("경인선(1호선)").region("\uACBD\uAE30").subRegion("부천시").stationName("부천").latitude(37.4841).longitude(126.7827).detailLocation("부천역 유실물센터").telNo("032-662-7788").build(),
            SubwayLostCenter.builder().operatorName("코레일").lineName("경의중앙선").region("\uACBD\uAE30").subRegion("고양시").stationName("일산").latitude(37.6821).longitude(126.7699).detailLocation("일산역").telNo("031-975-7788").build(),
            SubwayLostCenter.builder().operatorName("코레일").lineName("경춘선").region("\uACBD\uAE30").subRegion("남양주시").stationName("평내호평").latitude(37.6532).longitude(127.2443).detailLocation("평내호평역").telNo("031-591-7788").build(),
            
            SubwayLostCenter.builder().operatorName("인천교통공사").lineName("인천1,2호선").region("\uC778\uCC9C").subRegion("남동구").stationName("인천시청").latitude(37.4575).longitude(126.7022).detailLocation("인천시청역").telNo("032-451-3650").build(),
            SubwayLostCenter.builder().operatorName("인천교통공사").lineName("인천1호선").region("\uC778\uCC9C").subRegion("연수구").stationName("테크노파크").latitude(37.3822).longitude(126.6565).detailLocation("테크노파크역").telNo("032-451-3654").build(),

            // --- 지방 광역시 (Other Major Cities) ---
            SubwayLostCenter.builder().operatorName("대구교통공사").lineName("1,2호선").region("\uB300\uAD6C").subRegion("중구").stationName("반월당").latitude(35.8647).longitude(128.5933).detailLocation("반월당역").telNo("053-640-3330").build(),
            SubwayLostCenter.builder().operatorName("대구교통공사").lineName("3호선").region("\uB300\uAD6C").subRegion("중구").stationName("명덕").latitude(35.8592).longitude(128.5913).detailLocation("명덕역").telNo("053-640-3330").build(),
            
            SubwayLostCenter.builder().operatorName("대전교통공사").lineName("1호선").region("\uB300\uC804").subRegion("동구").stationName("대전역").latitude(36.3323).longitude(127.4342).detailLocation("대전역").telNo("042-539-3939").build(),
            SubwayLostCenter.builder().operatorName("대전교통공사").lineName("1호선").region("\uB300\uC804").subRegion("중구").stationName("서대전네거리").latitude(36.3235).longitude(127.4116).detailLocation("서대전네거리역").telNo("042-539-3939").build(),
            
            SubwayLostCenter.builder().operatorName("광주교통공사").lineName("1호선").region("\uAD11\uC8FC").subRegion("서구").stationName("상무").latitude(35.1461).longitude(126.8534).detailLocation("상무역").telNo("062-604-8550").build(),
            SubwayLostCenter.builder().operatorName("광주교통공사").lineName("1호선").region("\uAD11\uC8FC").subRegion("동구").stationName("금남로4가").latitude(35.1495).longitude(126.9142).detailLocation("금남로4가역").telNo("062-604-8550").build()
        ));

        log.info("Subway data initialization with Unicode safety finished.");
    }
}
