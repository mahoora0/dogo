package com.example.dogo.service.report;

import com.example.dogo.dto.ReportCreateRequest;
import com.example.dogo.entity.ChatMessage;
import com.example.dogo.entity.ChatRoom;
import com.example.dogo.entity.PostReport;
import com.example.dogo.entity.ReportReasonType;
import com.example.dogo.entity.ReportStatus;
import com.example.dogo.entity.ReportTargetType;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.ChatMessageRepository;
import com.example.dogo.repository.PostReportRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostReportServiceTest {

	@Mock
	private PostReportRepository postReportRepository;

	@Mock
	private LostItemRepository lostItemRepository;

	@Mock
	private FoundItemRepository foundItemRepository;

	@Mock
	private AnimalReportRepository animalReportRepository;

	@Mock
	private MissingPersonRepository missingPersonRepository;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private UserRepository userRepository;

	private PostReportService postReportService;

	@BeforeEach
	void setUp() {
		postReportService = new PostReportService(
				postReportRepository,
				lostItemRepository,
				foundItemRepository,
				animalReportRepository,
				missingPersonRepository,
				chatMessageRepository,
				userRepository
		);
	}

	@Test
	void createSavesLostItemReport() {
		User reporter = user(10L);
		LostItem item = lostItem(user(20L), 3L);
		when(lostItemRepository.findById(3L)).thenReturn(Optional.of(item));
		when(postReportRepository.existsByReporterUserNoAndTargetTypeAndTargetId(10L, ReportTargetType.LOST_ITEM, 3L)).thenReturn(false);

		String redirectUrl = postReportService.create(request(ReportTargetType.LOST_ITEM, 3L), reporter);

		ArgumentCaptor<PostReport> captor = ArgumentCaptor.forClass(PostReport.class);
		verify(postReportRepository).save(captor.capture());
		assertThat(redirectUrl).isEqualTo("/lost-items/3");
		assertThat(captor.getValue().getTargetOwnerNo()).isEqualTo(20L);
		assertThat(captor.getValue().getReasonType()).isEqualTo(ReportReasonType.FRAUD);
	}

	@Test
	void createRejectsPublicApiTarget() {
		User reporter = user(10L);
		LostItem policeItem = LostItem.fromPolice(
				"ATC001",
				"Police lost item",
				"content",
				"wallet",
				"wallet",
				null,
				"black",
				LocalDateTime.of(2026, 5, 1, 10, 0),
				"Seoul",
				"Gangnam",
				"010-0000-0000"
		);
		ReflectionTestUtils.setField(policeItem, "lostId", 4L);
		when(lostItemRepository.findById(4L)).thenReturn(Optional.of(policeItem));

		assertThatThrownBy(() -> postReportService.create(request(ReportTargetType.LOST_ITEM, 4L), reporter))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createRejectsDuplicateReport() {
		User reporter = user(10L);
		LostItem item = lostItem(user(20L), 3L);
		when(lostItemRepository.findById(3L)).thenReturn(Optional.of(item));
		when(postReportRepository.existsByReporterUserNoAndTargetTypeAndTargetId(10L, ReportTargetType.LOST_ITEM, 3L)).thenReturn(true);

		assertThatThrownBy(() -> postReportService.create(request(ReportTargetType.LOST_ITEM, 3L), reporter))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createRejectsOwnPost() {
		User owner = user(10L);
		LostItem item = lostItem(owner, 3L);
		when(lostItemRepository.findById(3L)).thenReturn(Optional.of(item));

		assertThatThrownBy(() -> postReportService.create(request(ReportTargetType.LOST_ITEM, 3L), owner))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createRejectsDeletedPost() {
		User reporter = user(10L);
		LostItem item = lostItem(user(20L), 3L);
		item.setDeleted(true);
		when(lostItemRepository.findById(3L)).thenReturn(Optional.of(item));

		assertThatThrownBy(() -> postReportService.create(request(ReportTargetType.LOST_ITEM, 3L), reporter))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createRejectsMissingPost() {
		User reporter = user(10L);
		when(lostItemRepository.findById(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> postReportService.create(request(ReportTargetType.LOST_ITEM, 3L), reporter))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createSavesChatMessageReport() {
		User reporter = user(10L);
		User sender = user(20L);
		ReflectionTestUtils.setField(sender, "nickname", "Sender");
		ChatMessage message = chatMessage(sender, 5L, 77L, "외부 송금 요청합니다");
		when(chatMessageRepository.findById(5L)).thenReturn(Optional.of(message));
		when(postReportRepository.existsByReporterUserNoAndTargetTypeAndTargetId(10L, ReportTargetType.CHAT_MESSAGE, 5L)).thenReturn(false);

		String redirectUrl = postReportService.create(request(ReportTargetType.CHAT_MESSAGE, 5L), reporter);

		ArgumentCaptor<PostReport> captor = ArgumentCaptor.forClass(PostReport.class);
		verify(postReportRepository).save(captor.capture());
		PostReport saved = captor.getValue();
		assertThat(redirectUrl).isEqualTo("/admin/reports/chat/77");
		assertThat(saved.getTargetType()).isEqualTo(ReportTargetType.CHAT_MESSAGE);
		assertThat(saved.getTargetOwnerNo()).isEqualTo(20L);
		assertThat(saved.getTargetUrl()).isEqualTo("/admin/reports/chat/77");
		assertThat(saved.getTargetTitle()).isEqualTo("Sender: 외부 송금 요청합니다");
	}

	@Test
	void createTruncatesLongChatMessagePreview() {
		User reporter = user(10L);
		User sender = user(20L);
		ReflectionTestUtils.setField(sender, "nickname", "Sender");
		ChatMessage message = chatMessage(sender, 5L, 77L, "가".repeat(1000));
		when(chatMessageRepository.findById(5L)).thenReturn(Optional.of(message));

		postReportService.create(request(ReportTargetType.CHAT_MESSAGE, 5L), reporter);

		ArgumentCaptor<PostReport> captor = ArgumentCaptor.forClass(PostReport.class);
		verify(postReportRepository).save(captor.capture());
		assertThat(captor.getValue().getTargetTitle().length()).isEqualTo(300);
	}

	@Test
	void createRejectsOwnChatMessage() {
		User owner = user(10L);
		ChatMessage message = chatMessage(owner, 5L, 77L, "내 메시지");
		when(chatMessageRepository.findById(5L)).thenReturn(Optional.of(message));

		assertThatThrownBy(() -> postReportService.create(request(ReportTargetType.CHAT_MESSAGE, 5L), owner))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createRejectsMissingChatMessage() {
		User reporter = user(10L);
		when(chatMessageRepository.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> postReportService.create(request(ReportTargetType.CHAT_MESSAGE, 5L), reporter))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void searchMapsReporterFieldsToDto() {
		User reporter = user(10L);
		ReflectionTestUtils.setField(reporter, "loginId", "reporter");
		ReflectionTestUtils.setField(reporter, "nickname", "Reporter");
		PostReport report = new PostReport(
				reporter,
				ReportTargetType.LOST_ITEM,
				3L,
				20L,
				"Lost wallet",
				"/lost-items/3",
				ReportReasonType.FRAUD,
				"detail"
		);
		ReflectionTestUtils.setField(report, "reportId", 7L);
		ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 5, 29, 10, 0));
		when(postReportRepository.findAll(any(Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(report)));

		var rows = postReportService.search(null, null, null, PageRequest.of(0, 20));

		assertThat(rows.getContent()).hasSize(1);
		assertThat(rows.getContent().get(0).reporterNo()).isEqualTo(10L);
		assertThat(rows.getContent().get(0).reporterLoginId()).isEqualTo("reporter");
		assertThat(rows.getContent().get(0).reporterNickname()).isEqualTo("Reporter");
	}

	@Test
	void updateStatusStoresHandlerAndMemo() {
		User reporter = user(10L);
		User handler = user(99L);
		PostReport report = new PostReport(
				reporter,
				ReportTargetType.LOST_ITEM,
				3L,
				20L,
				"Lost wallet",
				"/lost-items/3",
				ReportReasonType.FRAUD,
				"detail"
		);
		when(postReportRepository.findById(7L)).thenReturn(Optional.of(report));

		postReportService.updateStatus(7L, ReportStatus.RESOLVED, "Done", handler);

		assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
		assertThat(report.getAdminMemo()).isEqualTo("Done");
		assertThat(report.getHandler()).isEqualTo(handler);
		assertThat(report.getHandledAt()).isNotNull();
	}

	private ReportCreateRequest request(ReportTargetType targetType, Long targetId) {
		ReportCreateRequest request = new ReportCreateRequest();
		request.setTargetType(targetType);
		request.setTargetId(targetId);
		request.setReasonType(ReportReasonType.FRAUD);
		request.setReasonDetail("Asks for external payment.");
		return request;
	}

	private LostItem lostItem(User owner, Long id) {
		LostItem item = new LostItem(
				owner,
				"Lost wallet",
				"content",
				"wallet",
				"wallet",
				null,
				"black",
				LocalDateTime.of(2026, 5, 1, 10, 0),
				"Seoul",
				"Gangnam",
				"010-0000-0000"
		);
		ReflectionTestUtils.setField(item, "lostId", id);
		return item;
	}

	private ChatMessage chatMessage(User sender, Long messageId, Long roomId, String content) {
		ChatRoom room = new ChatRoom(lostItem(sender, 999L), sender, sender);
		ReflectionTestUtils.setField(room, "roomId", roomId);
		ChatMessage message = new ChatMessage(room, sender, content, ChatMessage.MessageType.TALK);
		ReflectionTestUtils.setField(message, "messageId", messageId);
		return message;
	}

	private User user(Long userNo) {
		User user = new User("user" + userNo + "@dogo.local", "User " + userNo, "010-0000-0000");
		ReflectionTestUtils.setField(user, "userNo", userNo);
		return user;
	}
}
