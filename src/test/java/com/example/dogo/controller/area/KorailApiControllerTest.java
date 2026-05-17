package com.example.dogo.controller.area;

import com.example.dogo.repository.area.KorailLostFoundCenterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KorailApiControllerTest {

    private final KorailLostFoundCenterRepository lostFoundCenterRepository = mock(KorailLostFoundCenterRepository.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new KorailApiController(lostFoundCenterRepository))
            .build();

    @Test
    void busanRegionDoesNotMatchOtherStationByLocationDetailsText() throws Exception {
        when(lostFoundCenterRepository.findAllWithCoordinates()).thenReturn(List.of(
                Map.of(
                        "id", 1L,
                        "stationName", "수원",
                        "subRegion", "경기 수원시",
                        "locationDetails", "(2F) 맞이방 3번출입구 크리스피도넛 / 부산미도어묵 매장",
                        "telNo", "031-240-7788",
                        "latitude", 37.2656,
                        "longitude", 127.0
                ),
                Map.of(
                        "id", 2L,
                        "stationName", "부산",
                        "subRegion", "부산 동구",
                        "locationDetails", "부산역 유실물센터",
                        "telNo", "051-440-2516",
                        "latitude", 35.1151,
                        "longitude", 129.0415
                )
        ));

        mockMvc.perform(get("/api/korail/lost-found").param("region", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].stationName").value("부산"));
    }
}
