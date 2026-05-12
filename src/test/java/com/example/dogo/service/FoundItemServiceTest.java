package com.example.dogo.service;

import com.example.dogo.dto.FoundItemCreateRequest;
import com.example.dogo.dto.FoundItemDetailView;
import com.example.dogo.dto.FoundItemView;
import com.example.dogo.entity.FoundItem;
import com.example.dogo.entity.FoundItemImage;
import com.example.dogo.entity.User;
import com.example.dogo.repository.FoundItemImageRepository;
import com.example.dogo.repository.FoundItemRepository;
import com.example.dogo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class FoundItemServiceTest {

	@Mock
	private FoundItemRepository foundItemRepository;

	@Mock
	private FoundItemImageRepository foundItemImageRepository;

	@Mock
	private UserRepository userRepository;

	@TempDir
	private Path uploadDir;

	private FoundItemService foundItemService;

	@BeforeEach
	void setUp() {
		foundItemService = new FoundItemService(
				foundItemRepository,
				foundItemImageRepository,
				userRepository,
				uploadDir.toString()
		);
	}

	@Test
	void searchReturnsListViewsWithFirstImage() {
		FoundItem foundItem = foundItem(1L, "검정 지갑을 주웠습니다", "카드가 들어 있습니다");
		when(foundItemRepository.search("지갑", "지갑", "강남", "KEEPING", PageRequest.of(0, 9)))
				.thenReturn(new PageImpl<>(List.of(foundItem)));
		when(foundItemImageRepository.findFirstByFoundItemOrderBySortOrderAscImageIdAsc(foundItem))
				.thenReturn(Optional.of(image(foundItem, "/uploads/found-items/wallet.jpg")));

		Page<FoundItemView> result = foundItemService.search("지갑", "지갑", "강남", "KEEPING", PageRequest.of(0, 9));

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(1L);
		assertThat(result.getContent().get(0).title()).isEqualTo("검정 지갑을 주웠습니다");
		assertThat(result.getContent().get(0).status()).isEqualTo("KEEPING");
		assertThat(result.getContent().get(0).imageUrl()).isEqualTo("/uploads/found-items/wallet.jpg");
	}

	@Test
	void getDetailReturnsContentAndPlaceholderImageWhenNoImagesExist() {
		FoundItem foundItem = foundItem(2L, "노트북을 주웠습니다", "파란 파우치에 들어 있습니다");
		when(foundItemRepository.findById(2L)).thenReturn(Optional.of(foundItem));
		when(foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(foundItem)).thenReturn(List.of());

		FoundItemDetailView result = foundItemService.getDetail(2L);

		assertThat(result.id()).isEqualTo(2L);
		assertThat(result.description()).isEqualTo("파란 파우치에 들어 있습니다");
		assertThat(result.imageUrls()).hasSize(1);
		assertThat(result.imageUrls().get(0)).startsWith("data:image/svg+xml");
	}

	@Test
	void createUsesLoginUserAndDoesNotCreateDevUser() {
		User loginUser = new User("login@dogo.local", "로그인 사용자", "010-9999-8888");
		when(foundItemRepository.save(any(FoundItem.class))).thenAnswer(invocation -> {
			FoundItem saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "foundId", 10L);
			return saved;
		});

		Long id = foundItemService.create(request(null, "검정 지갑", null), loginUser);

		assertThat(id).isEqualTo(10L);

		ArgumentCaptor<FoundItem> captor = ArgumentCaptor.forClass(FoundItem.class);
		verify(foundItemRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(loginUser);
		assertThat(captor.getValue().getTitle()).isEqualTo("검정 지갑");
		assertThat(captor.getValue().getContent()).isEqualTo("카드가 들어 있습니다");
		assertThat(captor.getValue().getStatus()).isEqualTo("KEEPING");
		verify(userRepository, never()).findByEmail(any());
		verify(foundItemImageRepository, never()).save(any());
	}

	@Test
	void createStoresUploadedImageMetadataAndFile() throws Exception {
		User user = new User("dev@dogo.local", "개발자 사용자", "010-0000-0000");
		when(userRepository.findByEmail("dev@dogo.local")).thenReturn(Optional.of(user));
		when(foundItemRepository.save(any(FoundItem.class))).thenAnswer(invocation -> {
			FoundItem saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "foundId", 11L);
			return saved;
		});
		MockMultipartFile image = new MockMultipartFile(
				"image",
				"wallet.JPG",
				"image/jpeg",
				"test-image".getBytes()
		);

		foundItemService.create(request("검정 지갑을 주웠습니다", "검정 지갑", image), null);

		ArgumentCaptor<FoundItemImage> captor = ArgumentCaptor.forClass(FoundItemImage.class);
		verify(foundItemImageRepository).save(captor.capture());

		String imageUrl = captor.getValue().getImageUrl();
		assertThat(imageUrl).startsWith("/uploads/found-items/");
		assertThat(imageUrl).endsWith(".jpg");
		assertThat(Files.exists(uploadDir.resolve("found-items").resolve(Path.of(imageUrl).getFileName()))).isTrue();
	}

	@Test
	void createThrowsWhenRequiredFieldsAreMissing() {
		FoundItemCreateRequest request = request("제목", " ", null);

		assertThatThrownBy(() -> foundItemService.create(request, null))
				.isInstanceOf(IllegalArgumentException.class);

		verify(foundItemRepository, never()).save(any());
	}

	private FoundItem foundItem(Long id, String title, String content) {
		FoundItem foundItem = new FoundItem(
				new User("tester@dogo.local", "테스터", "010-1111-2222"),
				title,
				"검정 지갑",
				"지갑",
				null,
				LocalDateTime.of(2026, 5, 8, 12, 0),
				"서울",
				"강남역",
				"강남경찰서",
				"검정",
				content
		);
		ReflectionTestUtils.setField(foundItem, "foundId", id);
		return foundItem;
	}

	private FoundItemImage image(FoundItem foundItem, String imageUrl) {
		return new FoundItemImage(
				foundItem,
				"wallet.jpg",
				"stored-wallet.jpg",
				imageUrl,
				"image/jpeg",
				100L,
				0
		);
	}

	private FoundItemCreateRequest request(String title, String itemName, MockMultipartFile image) {
		FoundItemCreateRequest request = new FoundItemCreateRequest();
		request.setTitle(title);
		request.setItemName(itemName);
		request.setCategoryMain("지갑");
		request.setFoundAt(LocalDateTime.of(2026, 5, 8, 12, 0));
		request.setFoundArea("서울");
		request.setFoundPlace("강남역");
		request.setKeepPlace("강남경찰서");
		request.setColorName("검정");
		request.setContent("카드가 들어 있습니다");
		request.setImage(image);
		return request;
	}
}
