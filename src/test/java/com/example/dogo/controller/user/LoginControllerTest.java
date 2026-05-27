package com.example.dogo.controller.user;

import com.example.dogo.entity.user.User;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.repository.user.UserSocialAccountRepository;
import com.example.dogo.service.user.ProfileService;
import com.example.dogo.service.OAuth2Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemImageRepository;
import com.example.dogo.repository.item.FoundItemImageRepository;
import com.example.dogo.repository.item.ItemMatchRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.animal.AnimalReportMatchRepository;
import com.example.dogo.repository.Support.InquiryRepository;
import com.example.dogo.repository.ChatMessageRepository;
import com.example.dogo.repository.ChatRoomRepository;
import com.example.dogo.repository.missing.MissingPersonImageRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSocialAccountRepository userSocialAccountRepository = mock(UserSocialAccountRepository.class);
    private final ProfileService profileService = mock(ProfileService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final LostItemRepository lostItemRepository = mock(LostItemRepository.class);
    private final FoundItemRepository foundItemRepository = mock(FoundItemRepository.class);
    private final LostItemImageRepository lostItemImageRepository = mock(LostItemImageRepository.class);
    private final FoundItemImageRepository foundItemImageRepository = mock(FoundItemImageRepository.class);
    private final ItemMatchRepository itemMatchRepository = mock(ItemMatchRepository.class);
    private final AnimalReportRepository animalReportRepository = mock(AnimalReportRepository.class);
    private final AnimalReportMatchRepository animalReportMatchRepository = mock(AnimalReportMatchRepository.class);
    private final InquiryRepository inquiryRepository = mock(InquiryRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final OAuth2Service oauth2Service = mock(OAuth2Service.class);
    private final MissingPersonRepository missingPersonRepository = mock(MissingPersonRepository.class);
    private final MissingPersonImageRepository missingPersonImageRepository = mock(MissingPersonImageRepository.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new LoginController(
                    userRepository,
                    userSocialAccountRepository,
                    profileService,
                    passwordEncoder,
                    lostItemRepository,
                    foundItemRepository,
                    lostItemImageRepository,
                    foundItemImageRepository,
                    itemMatchRepository,
                    animalReportRepository,
                    animalReportMatchRepository,
                    inquiryRepository,
                    chatMessageRepository,
                    chatRoomRepository,
                    oauth2Service,
                    missingPersonRepository,
                    missingPersonImageRepository
            ))
            .build();

    @Test
    void getUserProfileReturnsUserDataAndCounts() throws Exception {
        User user = mock(User.class);
        when(user.getNickname()).thenReturn("테스트유저");
        when(user.getProfileImageUrl()).thenReturn("/uploads/profiles/test.png");
        when(user.getEmail()).thenReturn("test@dogo.com");
        when(user.getPhone()).thenReturn("010-1234-5678");
        when(user.getRegDate()).thenReturn(LocalDateTime.of(2026, 5, 20, 10, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(lostItemRepository.findByUserAndDeletedFalseOrderByRegDateDesc(user)).thenReturn(Collections.emptyList());
        when(foundItemRepository.findByUserAndDeletedFalseOrderByRegDateDesc(user)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname", is("테스트유저")))
                .andExpect(jsonPath("$.profileImageUrl", is("/uploads/profiles/test.png")))
                .andExpect(jsonPath("$.email", is("test@dogo.com")))
                .andExpect(jsonPath("$.phone", is("010-1234-5678")))
                .andExpect(jsonPath("$.regDate", is("2026.05.20")))
                .andExpect(jsonPath("$.lostCount", is(0)))
                .andExpect(jsonPath("$.foundCount", is(0)));
    }

    @Test
    void getItemSimpleDetailFoundReturnsData() throws Exception {
        com.example.dogo.entity.item.FoundItem item = mock(com.example.dogo.entity.item.FoundItem.class);
        when(item.getTitle()).thenReturn("습득물 제목");
        when(item.getItemName()).thenReturn("지갑");
        when(item.getCategoryMain()).thenReturn("현금/지갑");
        when(item.getCategorySub()).thenReturn("지갑");
        when(item.getColorName()).thenReturn("검정색");
        when(item.getFoundPlace()).thenReturn("홍대입구역 2번 출구");
        when(item.getFoundAt()).thenReturn(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(item.getStatus()).thenReturn("KEEPING");
        when(item.getContent()).thenReturn("상세내용");

        when(foundItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(item)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/chat/item/FOUND/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("습득물 제목")))
                .andExpect(jsonPath("$.itemName", is("지갑")))
                .andExpect(jsonPath("$.category", is("현금/지갑 > 지갑")))
                .andExpect(jsonPath("$.color", is("검정색")))
                .andExpect(jsonPath("$.place", is("홍대입구역 2번 출구")))
                .andExpect(jsonPath("$.date", is("2026.05.20 10:00")))
                .andExpect(jsonPath("$.status", is("보관중")))
                .andExpect(jsonPath("$.content", is("상세내용")))
                .andExpect(jsonPath("$.imageUrl", is("/images/noImageSize.png")));
    }

    @Test
    void getItemSimpleDetailLostReturnsData() throws Exception {
        com.example.dogo.entity.item.LostItem item = mock(com.example.dogo.entity.item.LostItem.class);
        when(item.getTitle()).thenReturn("분실물 제목");
        when(item.getItemName()).thenReturn("핸드폰");
        when(item.getCategoryMain()).thenReturn("전자기기");
        when(item.getCategorySub()).thenReturn("핸드폰");
        when(item.getColorName()).thenReturn("흰색");
        when(item.getLostPlace()).thenReturn("강남역");
        when(item.getLostAt()).thenReturn(LocalDateTime.of(2026, 5, 20, 11, 30));
        when(item.getStatus()).thenReturn("WAITING");
        when(item.getContent()).thenReturn("아이폰 분실");

        when(lostItemRepository.findById(2L)).thenReturn(Optional.of(item));
        when(lostItemImageRepository.findByLostItemOrderBySortOrderAscImageIdAsc(item)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/chat/item/LOST/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("분실물 제목")))
                .andExpect(jsonPath("$.itemName", is("핸드폰")))
                .andExpect(jsonPath("$.category", is("전자기기 > 핸드폰")))
                .andExpect(jsonPath("$.color", is("흰색")))
                .andExpect(jsonPath("$.place", is("강남역")))
                .andExpect(jsonPath("$.date", is("2026.05.20 11:30")))
                .andExpect(jsonPath("$.status", is("대기중")))
                .andExpect(jsonPath("$.content", is("아이폰 분실")))
                .andExpect(jsonPath("$.imageUrl", is("/images/noImageSize.png")));
    }
}
