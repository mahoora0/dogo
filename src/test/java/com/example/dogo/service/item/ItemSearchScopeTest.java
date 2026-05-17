package com.example.dogo.service.item;

import com.example.dogo.dto.item.FoundItemView;
import com.example.dogo.dto.item.LostItemView;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ItemSearchScopeTest {

	@Autowired
	private LostItemService lostItemService;

	@Autowired
	private FoundItemService foundItemService;

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private FoundItemRepository foundItemRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void lostSearchAppliesKeywordOnlyToSelectedScope() {
		User user = userRepository.save(new User("lost-scope-test@dogo.local", "분실검색", "010-0000-0000"));
		lostItemRepository.save(lostItem(user, "강남에서 분실", "스코프지갑", "지갑", "서울", "강남역"));
		lostItemRepository.save(lostItem(user, "스코프지갑이라는 제목", "노트북", "전자기기", "부산", "서면역"));

		Page<LostItemView> itemCategoryResult = lostItemService.search(
				"스코프지갑",
				"ITEM_CATEGORY",
				null,
				null,
				null,
				PageRequest.of(0, 10)
		);
		Page<LostItemView> titlePlaceResult = lostItemService.search(
				"스코프지갑",
				"TITLE_PLACE",
				null,
				null,
				null,
				PageRequest.of(0, 10)
		);
		Page<LostItemView> allResult = lostItemService.search(
				"스코프지갑",
				"ALL",
				null,
				null,
				null,
				PageRequest.of(0, 10)
		);

		assertThat(itemCategoryResult.getTotalElements()).isEqualTo(1);
		assertThat(itemCategoryResult.getContent().get(0).name()).isEqualTo("스코프지갑");
		assertThat(titlePlaceResult.getTotalElements()).isEqualTo(1);
		assertThat(titlePlaceResult.getContent().get(0).title()).isEqualTo("스코프지갑이라는 제목");
		assertThat(allResult.getTotalElements()).isEqualTo(2);
	}

	@Test
	void foundSearchAppliesKeywordOnlyToSelectedScope() {
		User user = userRepository.save(new User("found-scope-test@dogo.local", "습득검색", "010-0000-0000"));
		foundItemRepository.save(foundItem(user, "서울역에서 습득", "스코프우산", "생활용품", "서울", "서울역", "서울역센터"));
		foundItemRepository.save(foundItem(user, "스코프우산이라는 제목", "카드지갑", "지갑", "대구", "동대구역", "대구센터"));

		Page<FoundItemView> itemCategoryResult = foundItemService.search(
				"스코프우산",
				"ITEM_CATEGORY",
				null,
				null,
				null,
				PageRequest.of(0, 10)
		);
		Page<FoundItemView> titlePlaceResult = foundItemService.search(
				"스코프우산",
				"TITLE_PLACE",
				null,
				null,
				null,
				PageRequest.of(0, 10)
		);
		Page<FoundItemView> allResult = foundItemService.search(
				"스코프우산",
				"ALL",
				null,
				null,
				null,
				PageRequest.of(0, 10)
		);

		assertThat(itemCategoryResult.getTotalElements()).isEqualTo(1);
		assertThat(itemCategoryResult.getContent().get(0).name()).isEqualTo("스코프우산");
		assertThat(titlePlaceResult.getTotalElements()).isEqualTo(1);
		assertThat(titlePlaceResult.getContent().get(0).title()).isEqualTo("스코프우산이라는 제목");
		assertThat(allResult.getTotalElements()).isEqualTo(2);
	}

	private LostItem lostItem(
			User user,
			String title,
			String itemName,
			String categoryMain,
			String lostArea,
			String lostPlace
	) {
		return new LostItem(
				user,
				title,
				"상세 내용",
				itemName,
				categoryMain,
				null,
				"검정",
				LocalDateTime.of(2026, 5, 16, 12, 0),
				lostArea,
				lostPlace,
				null
		);
	}

	private FoundItem foundItem(
			User user,
			String title,
			String itemName,
			String categoryMain,
			String foundArea,
			String foundPlace,
			String keepPlace
	) {
		return new FoundItem(
				user,
				title,
				itemName,
				categoryMain,
				null,
				LocalDateTime.of(2026, 5, 16, 12, 0),
				foundArea,
				foundPlace,
				keepPlace,
				"검정",
				"상세 내용",
				null
		);
	}
}
