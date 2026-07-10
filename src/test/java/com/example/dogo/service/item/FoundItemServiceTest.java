package com.example.dogo.service.item;

import com.example.dogo.dto.item.FoundItemCreateRequest;
import com.example.dogo.dto.item.FoundItemDetailView;
import com.example.dogo.dto.item.FoundItemView;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.FoundItemImage;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.item.FoundItemImageRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.service.match.FoundItemMatchRequestedEvent;
import com.example.dogo.service.match.ItemMatchService;
import com.example.dogo.service.police.sync.PoliceFoundItemDetailEnrichmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class FoundItemServiceTest {

	@Mock
	private FoundItemRepository foundItemRepository;

	@Mock
	private FoundItemImageRepository foundItemImageRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PoliceFoundItemDetailEnrichmentService policeFoundItemDetailEnrichmentService;

	@Mock
	private ItemMatchService itemMatchService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@TempDir
	private Path uploadDir;

	private FoundItemService foundItemService;

	@BeforeEach
	void setUp() {
		foundItemService = new FoundItemService(
				foundItemRepository,
				foundItemImageRepository,
				userRepository,
				policeFoundItemDetailEnrichmentService,
				itemMatchService,
				eventPublisher,
				uploadDir.toString()
		);
	}

	@Test
	void searchReturnsListViewsWithFirstImage() {
		FoundItem foundItem = foundItem(1L, "검정 지갑을 주웠습니다", "카드가 들어 있습니다");
		when(foundItemRepository.findAll(org.mockito.ArgumentMatchers.<Specification<FoundItem>>any(), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(foundItem)));
		when(foundItemImageRepository.findByFoundItemInOrderBySortOrderAscImageIdAsc(List.of(foundItem)))
				.thenReturn(List.of(image(foundItem, "/uploads/found-items/wallet.jpg")));

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
		assertThat(result.imageUrls().get(0)).isEqualTo("/images/noImageSize.png");
	}

	@Test
	void createUsesLoginUserAndDoesNotCreateDevUser() {
		User loginUser = new User("login@dogo.local", "로그인 사용자", "010-9999-8888");
		when(foundItemRepository.save(any(FoundItem.class))).thenAnswer(invocation -> {
			FoundItem saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "foundId", 10L);
			return saved;
		});

		Long id = foundItemService.create(request("검정 지갑을 주웠습니다", "검정 지갑", null), loginUser);

		assertThat(id).isEqualTo(10L);

		ArgumentCaptor<FoundItem> captor = ArgumentCaptor.forClass(FoundItem.class);
		verify(foundItemRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(loginUser);
		assertThat(captor.getValue().getTitle()).isEqualTo("검정 지갑을 주웠습니다");
		assertThat(captor.getValue().getContent()).isEqualTo("카드가 들어 있습니다");
		assertThat(captor.getValue().getFoundArea()).isEqualTo("서울특별시 강남구");
		assertThat(captor.getValue().getStatus()).isEqualTo("KEEPING");
		verify(userRepository, never()).findByEmail(any());
		verify(foundItemImageRepository, never()).save(any());

		verify(eventPublisher).publishEvent((Object) argThat(event ->
				event instanceof FoundItemMatchRequestedEvent e && e.foundId().equals(10L)));
	}

	@Test
	void createStoresUploadedImageMetadataAndFile() throws Exception {
		User user = new User("login@dogo.local", "로그인 사용자", "010-0000-0000");
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

		foundItemService.create(request("검정 지갑을 주웠습니다", "검정 지갑", image), user);

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

	@Test
	void getForEditThrowsForPoliceSourcedItem() {
		FoundItem policeItem = FoundItem.fromPolice("FD001", null, "경찰 습득물", null,
				"지갑", "지갑", null, null,
				LocalDateTime.of(2026, 5, 8, 12, 0), "서울", "강남역", "강남경찰서", null, null, null, "KEEPING");
		ReflectionTestUtils.setField(policeItem, "foundId", 6L);
		when(foundItemRepository.findById(6L)).thenReturn(Optional.of(policeItem));

		User loginUser = new User("user@dogo.local", "사용자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		assertThatThrownBy(() -> foundItemService.getForEdit(6L, loginUser))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("수정 권한이 없습니다.");
	}

	@Test
	void getForEditThrowsWhenOwnershipMismatch() {
		FoundItem item = foundItemWithOwner(7L, 1L, "서울특별시 강남구");
		when(foundItemRepository.findById(7L)).thenReturn(Optional.of(item));

		User otherUser = new User("other@dogo.local", "다른사용자", "010-9999-9999");
		ReflectionTestUtils.setField(otherUser, "userNo", 2L);

		assertThatThrownBy(() -> foundItemService.getForEdit(7L, otherUser))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("수정 권한이 없습니다.");
	}

	@Test
	void getForEditSplitsAreaAndReturnsEditData() {
		FoundItem item = foundItemWithOwner(8L, 1L, "서울특별시 강남구");
		when(foundItemRepository.findById(8L)).thenReturn(Optional.of(item));
		when(foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(item)).thenReturn(List.of());

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		var result = foundItemService.getForEdit(8L, loginUser);

		assertThat(result.id()).isEqualTo(8L);
		assertThat(result.foundAreaProvince()).isEqualTo("서울특별시");
		assertThat(result.foundAreaDistrict()).isEqualTo("강남구");
	}

	@Test
	void updateChangesItemFields() {
		FoundItem item = foundItemWithOwner(9L, 1L, "서울특별시 강남구");
		when(foundItemRepository.findById(9L)).thenReturn(Optional.of(item));

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		FoundItemCreateRequest req = request("수정된 제목", "수정된 지갑", null);
		req.setFoundAreaProvince("부산광역시");
		req.setFoundAreaDistrict("해운대구");

		foundItemService.update(9L, req, loginUser);

		assertThat(item.getTitle()).isEqualTo("수정된 제목");
		assertThat(item.getItemName()).isEqualTo("수정된 지갑");
		assertThat(item.getFoundArea()).isEqualTo("부산광역시 해운대구");
	}

	@Test
	void updateReplacesImagesWhenNewImagesUploaded() throws Exception {
		FoundItem item = foundItemWithOwner(10L, 1L, "서울");
		FoundItemImage oldImage = image(item, "/uploads/found-items/old.jpg");
		ReflectionTestUtils.setField(oldImage, "storedName", "old.jpg");

		Path foundDir = uploadDir.resolve("found-items");
		java.nio.file.Files.createDirectories(foundDir);
		java.nio.file.Files.writeString(foundDir.resolve("old.jpg"), "old-data");

		when(foundItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(item)).thenReturn(List.of(oldImage));

		User loginUser = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(loginUser, "userNo", 1L);

		MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new-data".getBytes());
		FoundItemCreateRequest req = request("제목", "지갑", newImage);

		foundItemService.update(10L, req, loginUser);

		verify(foundItemImageRepository).deleteAll(List.of(oldImage));
		assertThat(java.nio.file.Files.exists(foundDir.resolve("old.jpg"))).isFalse();
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
				content,
				"02-0000-0000"
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

	private FoundItem foundItemWithOwner(Long id, Long ownerUserNo, String foundArea) {
		User owner = new User("owner@dogo.local", "소유자", "010-0000-0001");
		ReflectionTestUtils.setField(owner, "userNo", ownerUserNo);
		FoundItem item = new FoundItem(owner, "검정 지갑을 주웠습니다", "검정 지갑",
				"지갑", null, LocalDateTime.of(2026, 5, 8, 12, 0),
				foundArea, "강남역", "강남경찰서", "검정", null, null);
		ReflectionTestUtils.setField(item, "foundId", id);
		return item;
	}

	private FoundItemCreateRequest request(String title, String itemName, MockMultipartFile image) {
		FoundItemCreateRequest request = new FoundItemCreateRequest();
		request.setTitle(title);
		request.setItemName(itemName);
		request.setCategoryMain("지갑");
		request.setFoundAt(LocalDateTime.of(2026, 5, 8, 12, 0));
		request.setFoundAreaProvince("서울특별시");
		request.setFoundAreaDistrict("강남구");
		request.setFoundPlace("강남역");
		request.setKeepPlace("강남경찰서");
		request.setColorName("검정");
		request.setContent("카드가 들어 있습니다");
		request.setImage(image);
		return request;
	}
}
