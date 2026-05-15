package com.example.dogo.controller.area;

import com.example.dogo.entity.area.PoliceStation;
import com.example.dogo.repository.area.PoliceStationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PoliceApiControllerTest {

    private final PoliceStationRepository policeStationRepository = mock(PoliceStationRepository.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PoliceApiController(policeStationRepository))
            .build();

    @Test
    void neighborhoodsRefreshFromSelectedSubRegionUsingAddressFields() throws Exception {
        when(policeStationRepository.findByPnuStartingWith("26")).thenReturn(List.of(
                station("2611010100", "부산광역시 중구 남포동", "부산광역시 중구 남포동", "부산 중구"),
                station("2626010100", null, "부산광역시 동래구 온천동", "부산 동래구")
        ));

        mockMvc.perform(get("/api/police/neighborhoods")
                        .param("region", "부산")
                        .param("subRegion", "동래구"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("온천동")));
    }

    private PoliceStation station(String pnu, String addr, String address1, String cmptncRgnNm) {
        return new PoliceStation(
                pnu,
                129.0,
                35.0,
                null,
                address1,
                null,
                null,
                cmptncRgnNm,
                "테스트지구대",
                "지구대",
                "051-000-0000",
                addr
        );
    }
}
