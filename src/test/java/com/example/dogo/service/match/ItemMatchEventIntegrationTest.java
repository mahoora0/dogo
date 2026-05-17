package com.example.dogo.service.match;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.ItemMatch;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.ItemMatchRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.sql.init.mode=never"
})
class ItemMatchEventIntegrationTest {

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private FoundItemRepository foundItemRepository;

	@Autowired
	private ItemMatchRepository itemMatchRepository;

	@Test
	void matchEventStoresMatchAfterOuterTransactionCommits() throws Exception {
		Long lostId = transactionTemplate.execute(status -> {
			User user = userRepository.save(new User("event-test@dogo.local", "이벤트테스트", "010-1111-2222"));
			LocalDateTime lostAt = LocalDateTime.of(2026, 5, 10, 18, 0);

			FoundItem foundItem = foundItemRepository.save(new FoundItem(
					user,
					"블랙 지갑을 주웠습니다",
					"블랙 지갑",
					"지갑",
					null,
					lostAt.plusHours(2),
					"서울",
					"강남역",
					"강남경찰서",
					"블랙",
					"카드가 들어 있는 검정 지갑입니다",
					"02-0000-0000"
			));
			LostItem lostItem = lostItemRepository.save(new LostItem(
					user,
					"검정 지갑을 찾습니다",
					"카드가 들어 있는 검정 지갑입니다",
					"검정 지갑",
					"지갑",
					null,
					"검정",
					lostAt,
					"서울",
					"강남역",
					"010-0000-0000"
			));

			eventPublisher.publishEvent(new LostItemMatchRequestedEvent(lostItem.getLostId()));
			return lostItem.getLostId();
		});

		List<ItemMatch> matches = waitForMatches(lostId);

		assertThat(matches).hasSize(1);
		ItemMatch match = matches.get(0);
		assertThat(match.getFoundItem().getFoundId()).isNotNull();
		assertThat(match.getRuleScore()).isEqualByComparingTo(match.getFinalScore());
		assertThat(match.getSemanticScore()).isNull();
		assertThat(match.getMatchVersion()).isEqualTo("java-rule-v1");
		assertThat(match.getMatchReasonList()).isNotEmpty();
	}

	private List<ItemMatch> waitForMatches(Long lostId) throws InterruptedException {
		long deadline = System.nanoTime() + 5_000_000_000L;
		List<ItemMatch> matches = List.of();
		while (System.nanoTime() < deadline) {
			matches = itemMatchRepository.findByLostIdWithFoundItem(lostId);
			if (!matches.isEmpty()) {
				return matches;
			}
			Thread.sleep(100);
		}
		return matches;
	}
}
