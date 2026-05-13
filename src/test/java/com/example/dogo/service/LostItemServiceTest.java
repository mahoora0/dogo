package com.example.dogo.service;

import com.example.dogo.dto.LostItemCreateRequest;
import com.example.dogo.dto.LostItemDetailView;
import com.example.dogo.dto.LostItemView;
import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.LostItemImage;
import com.example.dogo.entity.User;
import com.example.dogo.repository.LostItemImageRepository;
import com.example.dogo.repository.LostItemRepository;
import com.example.dogo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LostItemServiceTest {

	@Mock
	private LostItemRepository lostItemRepository;

	@Mock
	private LostItemImageRepository lostItemImageRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PoliceLostItemDetailEnrichmentService policeLostItemDetailEnrichmentService;

	@Mock
	private ItemMatchService itemMatchService;

	@TempDir
	private Path uploadDir;

	private LostItemService lostItemService;

	@BeforeEach
	void setUp() {
		lostItemService = new LostItemService(
				lostItemRepository,
				lostItemImageRepository,
				userRepository,
				policeLostItemDetailEnrichmentService,
				itemMatchService,
				uploadDir.toString()
		);
	}

	@Test
	void searchReturnsListViewsWithFirstImage() {
		LostItem lostItem = lostItem(1L, "검정 지갑을 찾습니다", "카드가 들어있습니다");
		when(lostItemRepository.search("지갑", "지갑", "서울", "WAITING")).thenReturn(List.of(lostItem));
		when(lostItemImageRepository.findFirstByLostItemOrderBySortOrderAscImageIdAsc(lostItem))
				.thenReturn(Optional.of(image(lostItem, "/uploads/lost-items/wallet.jpg")));

		List<LostItemView> result = lostItemService.search("지갑", "지갑", "서울", "WAITING");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(1L);
		assertThat(result.get(0).title()).isEqualTo("검정 지갑을 찾습니다");
		assertThat(result.get(0).statusLabel()).isEqualTo("대기중");
		assertThat(result.get(0).imageUrl()).isEqualTo("/uploads/lost-items/wallet.jpg");
	}

	@Test
	void getDetailReturnsPlaceholderImageWhenNoImagesExist() {
		LostItem lostItem = lostItem(2L, "노트북을 찾습니다", "회색 파우치");
		when(lostItemRepository.findById(2L)).thenReturn(Optional.of(lostItem));
		when(lostItemImageRepository.findByLostItemOrderBySortOrderAscImageIdAsc(lostItem)).thenReturn(List.of());

		LostItemDetailView result = lostItemService.getDetail(2L);

		assertThat(result.id()).isEqualTo(2L);
		assertThat(result.imageUrls()).hasSize(1);
		assertThat(result.imageUrls().get(0)).isEqualTo("/images/noImageSize.png");
	}

	@Test
	void getDetailThrowsWhenLostItemDoesNotExist() {
		when(lostItemRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> lostItemService.getDetail(99L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("분실물 게시글을 찾을 수 없습니다.");
	}

	@Test
	void createSavesLostItemAndSkipsEmptyImage() {
		User user = new User("dev@dogo.local", "개발용 사용자", "010-0000-0000");
		when(userRepository.findByEmail("dev@dogo.local")).thenReturn(Optional.of(user));
		when(lostItemRepository.save(any(LostItem.class))).thenAnswer(invocation -> {
			LostItem saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "lostId", 10L);
			return saved;
		});

		LostItemCreateRequest request = request(null, "검정 지갑", null);

		Long id = lostItemService.create(request, null);

		assertThat(id).isEqualTo(10L);

		ArgumentCaptor<LostItem> captor = ArgumentCaptor.forClass(LostItem.class);
		verify(lostItemRepository).save(captor.capture());
		assertThat(captor.getValue().getTitle()).isEqualTo("검정 지갑");
		assertThat(captor.getValue().getContent()).isNull();
		assertThat(captor.getValue().getStatus()).isEqualTo("WAITING");
		verify(lostItemImageRepository, never()).save(any());
	}

	@Test
	void createStoresUploadedImageMetadataAndFile() throws Exception {
		User user = new User("dev@dogo.local", "개발용 사용자", "010-0000-0000");
		when(userRepository.findByEmail("dev@dogo.local")).thenReturn(Optional.of(user));
		when(lostItemRepository.save(any(LostItem.class))).thenAnswer(invocation -> {
			LostItem saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "lostId", 11L);
			return saved;
		});
		MockMultipartFile image = new MockMultipartFile(
				"image",
				"wallet.JPG",
				"image/jpeg",
				"test-image".getBytes()
		);

		lostItemService.create(request("검정 지갑을 찾습니다", "검정 지갑", image), null);

		ArgumentCaptor<LostItemImage> captor = ArgumentCaptor.forClass(LostItemImage.class);
		verify(lostItemImageRepository).save(captor.capture());

		String imageUrl = captor.getValue().getImageUrl();
		assertThat(imageUrl).startsWith("/uploads/lost-items/");
		assertThat(imageUrl).endsWith(".jpg");
		assertThat(Files.exists(uploadDir.resolve("lost-items").resolve(Path.of(imageUrl).getFileName()))).isTrue();
	}

	private LostItem lostItem(Long id, String title, String content) {
		LostItem lostItem = new LostItem(
				new User("tester@dogo.local", "테스터", "010-1111-2222"),
				title,
				content,
				"검정 지갑",
				"지갑",
				null,
				LocalDateTime.of(2026, 5, 8, 12, 0),
				"서울",
				"강남역",
				"010-1234-5678"
		);
		ReflectionTestUtils.setField(lostItem, "lostId", id);
		return lostItem;
	}

	private LostItemImage image(LostItem lostItem, String imageUrl) {
		return new LostItemImage(
				lostItem,
				"wallet.jpg",
				"stored-wallet.jpg",
				imageUrl,
				"image/jpeg",
				100L,
				0
		);
	}

	private LostItemCreateRequest request(String title, String itemName, MockMultipartFile image) {
		LostItemCreateRequest request = new LostItemCreateRequest();
		request.setTitle(title);
		request.setItemName(itemName);
		request.setCategoryMain("지갑");
		request.setLostAt(LocalDateTime.of(2026, 5, 8, 12, 0));
		request.setLostArea("서울");
		request.setLostPlace("강남역");
		request.setContact("010-1234-5678");
		request.setContent(" ");
		request.setImage(image);
		return request;
	}
}
