package com.example.dogo.service.item;

import com.example.dogo.dto.item.LostItemCreateRequest;
import com.example.dogo.dto.item.LostItemDetailView;
import com.example.dogo.dto.item.LostItemView;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.item.LostItemImage;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.item.LostItemImageRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.service.match.ItemMatchService;
import com.example.dogo.service.match.LostItemMatchRequestedEvent;
import com.example.dogo.service.match.MatchTextNormalizer;
import com.example.dogo.service.police.sync.PoliceLostItemDetailEnrichmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import static org.mockito.ArgumentMatchers.argThat;
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

	@Mock
	private ApplicationEventPublisher eventPublisher;

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
				eventPublisher,
				new MatchTextNormalizer(),
				uploadDir.toString()
		);
	}

	@Test
	void searchReturnsListViewsWithFirstImage() {
		LostItem lostItem = lostItem(1L, "검정 지갑을 찾습니다", "카드가 들어있습니다");
		when(lostItemRepository.findAll(org.mockito.ArgumentMatchers.<Specification<LostItem>>any(), any(Sort.class)))
				.thenReturn(List.of(lostItem));
		when(lostItemImageRepository.findFirstByLostItemOrderBySortOrderAscImageIdAsc(lostItem))
				.thenReturn(Optional.of(image(lostItem, "/uploads/lost-items/wallet.jpg")));

		List<LostItemView> result = lostItemService.search("지갑", "지갑", "서울", "WAITING");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo(1L);
		assertThat(result.get(0).title()).isEqualTo("검정 지갑을 찾습니다");
		assertThat(result.get(0).statusLabel()).isEqualTo("대기중");
		assertThat(result.get(0).colorName()).isEqualTo("블랙(검정)");
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

		LostItemCreateRequest request = request("검정 지갑을 찾습니다", "검정 지갑", null);

		Long id = lostItemService.create(request, null);

		assertThat(id).isEqualTo(10L);

		ArgumentCaptor<LostItem> captor = ArgumentCaptor.forClass(LostItem.class);
		verify(lostItemRepository).save(captor.capture());
		assertThat(captor.getValue().getTitle()).isEqualTo("검정 지갑을 찾습니다");
		assertThat(captor.getValue().getContent()).isNull();
		assertThat(captor.getValue().getColorName()).isEqualTo("검정");
		assertThat(captor.getValue().getLostArea()).isEqualTo("서울특별시 강남구");
		assertThat(captor.getValue().getStatus()).isEqualTo("WAITING");
		verify(lostItemImageRepository, never()).save(any());

		verify(eventPublisher).publishEvent((Object) argThat(event ->
				event instanceof LostItemMatchRequestedEvent e && e.lostId().equals(10L)));
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

	@Test
	void getForEditSplitsAreaAndReturnsEditData() {
		LostItem item = lostItemWithOwner(5L, 1L, "서울특별시 강남구");
		when(lostItemRepository.findById(5L)).thenReturn(Optional.of(item));
		when(lostItemImageRepository.findByLostItemOrderBySortOrderAscImageIdAsc(item))
				.thenReturn(List.of(image(item, "/uploads/lost-items/wallet.jpg")));

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		var result = lostItemService.getForEdit(5L, loginUser);

		assertThat(result.id()).isEqualTo(5L);
		assertThat(result.lostAreaProvince()).isEqualTo("서울특별시");
		assertThat(result.lostAreaDistrict()).isEqualTo("강남구");
		assertThat(result.existingImageUrls()).containsExactly("/uploads/lost-items/wallet.jpg");
	}

	@Test
	void getForEditThrowsWhenItemNotFound() {
		when(lostItemRepository.findById(99L)).thenReturn(Optional.empty());

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		assertThatThrownBy(() -> lostItemService.getForEdit(99L, loginUser))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("분실물 게시글을 찾을 수 없습니다.");
	}

	@Test
	void getForEditThrowsForPoliceSourcedItem() {
		LostItem policeItem = LostItem.fromPolice("ATC001", "경찰 등록 분실물", null,
				"지갑", "지갑", null, null,
				LocalDateTime.of(2026, 5, 8, 12, 0), "서울", "강남역", null);
		ReflectionTestUtils.setField(policeItem, "lostId", 6L);
		when(lostItemRepository.findById(6L)).thenReturn(Optional.of(policeItem));

		User loginUser = new User("user@dogo.local", "사용자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		assertThatThrownBy(() -> lostItemService.getForEdit(6L, loginUser))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("수정 권한이 없습니다.");
	}

	@Test
	void getForEditThrowsWhenOwnershipMismatch() {
		LostItem item = lostItemWithOwner(7L, 1L, "서울특별시 강남구");
		when(lostItemRepository.findById(7L)).thenReturn(Optional.of(item));

		User otherUser = new User("other@dogo.local", "다른사용자", "010-9999-9999");
		ReflectionTestUtils.setField(otherUser, "userNo", 2L);

		assertThatThrownBy(() -> lostItemService.getForEdit(7L, otherUser))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("수정 권한이 없습니다.");
	}

	@Test
	void updateChangesItemFields() {
		LostItem item = lostItemWithOwner(8L, 1L, "서울특별시 강남구");
		when(lostItemRepository.findById(8L)).thenReturn(Optional.of(item));

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		LostItemCreateRequest req = request("수정된 제목", "수정된 지갑", null);
		req.setLostAreaProvince("부산광역시");
		req.setLostAreaDistrict("해운대구");

		lostItemService.update(8L, req, loginUser);

		assertThat(item.getTitle()).isEqualTo("수정된 제목");
		assertThat(item.getItemName()).isEqualTo("수정된 지갑");
		assertThat(item.getLostArea()).isEqualTo("부산광역시 해운대구");
	}

	@Test
	void updateReplacesImagesWhenNewImagesUploaded() throws Exception {
		LostItem item = lostItemWithOwner(9L, 1L, "서울");
		LostItemImage oldImage = image(item, "/uploads/lost-items/old.jpg");
		ReflectionTestUtils.setField(oldImage, "storedName", "old.jpg");

		Path lostDir = uploadDir.resolve("lost-items");
		java.nio.file.Files.createDirectories(lostDir);
		java.nio.file.Files.writeString(lostDir.resolve("old.jpg"), "old-data");

		when(lostItemRepository.findById(9L)).thenReturn(Optional.of(item));
		when(lostItemImageRepository.findByLostItemOrderBySortOrderAscImageIdAsc(item)).thenReturn(List.of(oldImage));

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new-data".getBytes());
		LostItemCreateRequest req = request("제목", "지갑", newImage);

		lostItemService.update(9L, req, loginUser);

		verify(lostItemImageRepository).deleteAll(List.of(oldImage));
		assertThat(java.nio.file.Files.exists(lostDir.resolve("old.jpg"))).isFalse();

		ArgumentCaptor<LostItemImage> captor = ArgumentCaptor.forClass(LostItemImage.class);
		verify(lostItemImageRepository).save(captor.capture());
		assertThat(captor.getValue().getImageUrl()).startsWith("/uploads/lost-items/");
	}

	@Test
	void updateKeepsExistingImagesWhenNoNewImages() {
		LostItem item = lostItemWithOwner(10L, 1L, "서울");
		when(lostItemRepository.findById(10L)).thenReturn(Optional.of(item));

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		LostItemCreateRequest req = request("제목", "지갑", null);
		lostItemService.update(10L, req, loginUser);

		verify(lostItemImageRepository, never()).deleteAll(any());
		verify(lostItemImageRepository, never()).save(any());
	}

	private LostItem lostItem(Long id, String title, String content) {
		LostItem lostItem = new LostItem(
				new User("tester@dogo.local", "테스터", "010-1111-2222"),
				title,
				content,
					"검정 지갑",
					"지갑",
					null,
					null,
					LocalDateTime.of(2026, 5, 8, 12, 0),
				"서울",
				"강남역",
				"010-1234-5678"
		);
		ReflectionTestUtils.setField(lostItem, "lostId", id);
		return lostItem;
	}

	private LostItem lostItemWithOwner(Long id, Long ownerUserNo, String lostArea) {
		User owner = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(owner, "userNo", ownerUserNo);
		LostItem item = new LostItem(owner, "검정 지갑을 찾습니다", null,
				"검정 지갑", "지갑", null, null,
				LocalDateTime.of(2026, 5, 8, 12, 0), lostArea, "강남역", "010-1234-5678");
		ReflectionTestUtils.setField(item, "lostId", id);
		return item;
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
		request.setColorName("검정");
		request.setLostAt(LocalDateTime.of(2026, 5, 8, 12, 0));
		request.setLostAreaProvince("서울특별시");
		request.setLostAreaDistrict("강남구");
		request.setLostPlace("강남역");
		request.setContact("010-1234-5678");
		request.setContent(" ");
		request.setImage(image);
		return request;
	}
}
