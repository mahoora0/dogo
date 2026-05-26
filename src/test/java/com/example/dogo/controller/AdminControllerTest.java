package com.example.dogo.controller;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.Support.InquiryRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private final AnimalReportRepository animalReportRepository = mock(AnimalReportRepository.class);
    private final MissingPersonRepository missingPersonRepository = mock(MissingPersonRepository.class);
    private final LostItemRepository lostItemRepository = mock(LostItemRepository.class);
    private final FoundItemRepository foundItemRepository = mock(FoundItemRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final InquiryRepository inquiryRepository = mock(InquiryRepository.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AdminController(
                    animalReportRepository,
                    missingPersonRepository,
                    lostItemRepository,
                    foundItemRepository,
                    userRepository,
                    inquiryRepository
            ))
            .build();

    @Test
    void toggleEmergencyChangesActiveStatusAndReturnsIt() throws Exception {
        // Initially status might be false, let's call status to verify or toggle directly
        mockMvc.perform(get("/admin/api/emergency/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        // 1st Toggle -> should become true
        mockMvc.perform(post("/admin/api/emergency/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));

        // 2nd Status -> should return true with animal details (mocking fallback or actual report)
        when(animalReportRepository.findByDeletedFalseOrderByRegdateDesc()).thenReturn(Collections.emptyList());
        when(missingPersonRepository.findByDeletedFalseOrderByRegdateDesc()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/admin/api/emergency/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.title", is("실종된 반려동물을 찾습니다!")))
                .andExpect(jsonPath("$.breed", is("리트리버")));

        // 3rd Toggle -> should become false again
        mockMvc.perform(post("/admin/api/emergency/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        // 4th Status -> should return false
        mockMvc.perform(get("/admin/api/emergency/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void statusReturnsLatestAnimalReportWhenActive() throws Exception {
        // Make sure it is active
        mockMvc.perform(post("/admin/api/emergency/toggle"));

        AnimalReport mockReport = mock(AnimalReport.class);
        when(mockReport.getReportType()).thenReturn("MISSING");
        when(mockReport.getReportId()).thenReturn(42L);
        when(mockReport.getTitle()).thenReturn("우리 초코를 찾습니다!");
        when(mockReport.getBreedName()).thenReturn("푸들");
        when(mockReport.getAnimalType()).thenReturn("개");
        when(mockReport.getRegionName()).thenReturn("서울시");
        when(mockReport.getDetailPlace()).thenReturn("마포구");
        when(mockReport.getEventDate()).thenReturn(LocalDate.of(2026, 5, 18));
        when(mockReport.getDistinctiveMarks()).thenReturn("귀가 쫑긋하고 빨간 목줄을 함");
        when(mockReport.getImages()).thenReturn(Collections.emptyList());
        when(mockReport.getRegdate()).thenReturn(LocalDateTime.of(2026, 5, 18, 12, 0));

        when(animalReportRepository.findByDeletedFalseOrderByRegdateDesc()).thenReturn(List.of(mockReport));
        when(missingPersonRepository.findByDeletedFalseOrderByRegdateDesc()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/api/emergency/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.type", is("ANIMAL")))
                .andExpect(jsonPath("$.reportId", is(42)))
                .andExpect(jsonPath("$.title", is("우리 초코를 찾습니다!")))
                .andExpect(jsonPath("$.breed", is("푸들")))
                .andExpect(jsonPath("$.location", is("서울시 마포구")))
                .andExpect(jsonPath("$.distinctiveMarks", is("귀가 쫑긋하고 빨간 목줄을 함")));

        // Turn off emergency to leave state clean
        mockMvc.perform(post("/admin/api/emergency/toggle"));
    }

    @Test
    void statusReturnsLatestPersonReportWhenPersonIsMoreRecent() throws Exception {
        // Make sure it is active
        mockMvc.perform(post("/admin/api/emergency/toggle"));

        AnimalReport mockAnimal = mock(AnimalReport.class);
        when(mockAnimal.getReportType()).thenReturn("MISSING");
        when(mockAnimal.getRegdate()).thenReturn(LocalDateTime.of(2026, 5, 18, 12, 0));

        MissingPersonReport mockPerson = mock(MissingPersonReport.class);
        when(mockPerson.getStatus()).thenReturn("OPEN");
        when(mockPerson.getReportId()).thenReturn(77L);
        when(mockPerson.getAge()).thenReturn(15);
        when(mockPerson.getNationality()).thenReturn("대한민국");
        when(mockPerson.getOccurredPlace()).thenReturn("서울 강남역 10번출구");
        when(mockPerson.getOccurredAt()).thenReturn(LocalDateTime.of(2026, 5, 19, 10, 0));
        when(mockPerson.getClothing()).thenReturn("검은색 반팔, 청바지");
        when(mockPerson.getBodyType()).thenReturn("보통");
        when(mockPerson.getFaceShape()).thenReturn("둥근형");
        when(mockPerson.getHairColor()).thenReturn("갈색");
        when(mockPerson.getHairStyle()).thenReturn("단발");
        when(mockPerson.getRegdate()).thenReturn(LocalDateTime.of(2026, 5, 19, 11, 0)); // More recent!

        when(animalReportRepository.findByDeletedFalseOrderByRegdateDesc()).thenReturn(List.of(mockAnimal));
        when(missingPersonRepository.findByDeletedFalseOrderByRegdateDesc()).thenReturn(List.of(mockPerson));

        mockMvc.perform(get("/admin/api/emergency/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.type", is("PERSON")))
                .andExpect(jsonPath("$.reportId", is(77)))
                .andExpect(jsonPath("$.title", is("실종자를 찾습니다!")))
                .andExpect(jsonPath("$.age", is(15)))
                .andExpect(jsonPath("$.nationality", is("대한민국")))
                .andExpect(jsonPath("$.location", is("서울 강남역 10번출구")))
                .andExpect(jsonPath("$.distinctiveMarks", is("의상: 검은색 반팔, 청바지\n체형: 보통, 얼굴형: 둥근형, 헤어: 갈색 (단발)")));

        // Turn off emergency to leave state clean
        mockMvc.perform(post("/admin/api/emergency/toggle"));
    }

    @Test
    void toggleSettingSuccessfullyChangesConfig() throws Exception {
        mockMvc.perform(post("/admin/api/settings/toggle")
                        .param("key", "location_weight_enabled")
                        .param("value", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.key", is("location_weight_enabled")))
                .andExpect(jsonPath("$.value", is(false)));
    }
}
