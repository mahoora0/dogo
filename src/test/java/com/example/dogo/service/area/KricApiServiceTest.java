package com.example.dogo.service.area;

import com.example.dogo.dto.area.SubwayLostCenterDTO;
import com.example.dogo.entity.area.SubwayLostCenter;
import com.example.dogo.repository.area.SubwayLostCenterRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KricApiServiceTest {

    private final SubwayLostCenterRepository repository = mock(SubwayLostCenterRepository.class);
    private final KricApiService kricApiService = new KricApiService(repository);

    @Test
    void subRegionSelectionExcludesCentersFromOtherDistricts() {
        when(repository.findAll()).thenReturn(List.of(
                SubwayLostCenter.builder()
                        .operatorName("부산교통공사")
                        .lineName("1호선")
                        .region("부산")
                        .subRegion("중구")
                        .stationName("남포")
                        .latitude(35.0978)
                        .longitude(129.0348)
                        .detailLocation("남포역")
                        .telNo("051-640-7330")
                        .build(),
                SubwayLostCenter.builder()
                        .operatorName("부산교통공사")
                        .lineName("1호선")
                        .region("부산")
                        .subRegion("동래구")
                        .stationName("동래")
                        .latitude(35.2056)
                        .longitude(129.0786)
                        .detailLocation("동래역")
                        .telNo("051-640-7331")
                        .build()
        ));

        List<SubwayLostCenterDTO> result = kricApiService.getSubwayList("부산", "동래구", null, null);

        assertThat(result)
                .extracting(SubwayLostCenterDTO::getStationName)
                .containsExactly("동래");
    }

    @Test
    void regionSelectionExcludesCentersFromOtherCities() {
        when(repository.findAll()).thenReturn(List.of(
                SubwayLostCenter.builder()
                        .operatorName("부산교통공사")
                        .lineName("2호선")
                        .region("부산")
                        .subRegion("해운대구")
                        .stationName("해운대")
                        .latitude(35.1636)
                        .longitude(129.1585)
                        .detailLocation("해운대역")
                        .telNo("051-640-7332")
                        .build(),
                SubwayLostCenter.builder()
                        .operatorName("대구교통공사")
                        .lineName("1,2호선")
                        .region("대구")
                        .subRegion("중구")
                        .stationName("반월당")
                        .latitude(35.8647)
                        .longitude(128.5933)
                        .detailLocation("반월당역")
                        .telNo("053-640-3330")
                        .build()
        ));

        List<SubwayLostCenterDTO> result = kricApiService.getSubwayList("대구", null, null, null);

        assertThat(result)
                .extracting(SubwayLostCenterDTO::getStationName)
                .containsExactly("반월당");
    }
}
