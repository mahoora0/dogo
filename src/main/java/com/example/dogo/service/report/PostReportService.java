package com.example.dogo.service.report;

import com.example.dogo.dto.ReportCreateRequest;
import com.example.dogo.dto.AdminReportRow;
import com.example.dogo.entity.ChatMessage;
import com.example.dogo.entity.PostReport;
import com.example.dogo.entity.ReportReasonType;
import com.example.dogo.entity.ReportStatus;
import com.example.dogo.entity.ReportTargetType;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.ChatMessageRepository;
import com.example.dogo.repository.PostReportRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PostReportService {

	private static final int MAX_REASON_DETAIL_LENGTH = 1000;

	private final PostReportRepository postReportRepository;
	private final LostItemRepository lostItemRepository;
	private final FoundItemRepository foundItemRepository;
	private final AnimalReportRepository animalReportRepository;
	private final MissingPersonRepository missingPersonRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final UserRepository userRepository;

	public PostReportService(
			PostReportRepository postReportRepository,
			LostItemRepository lostItemRepository,
			FoundItemRepository foundItemRepository,
			AnimalReportRepository animalReportRepository,
			MissingPersonRepository missingPersonRepository,
			ChatMessageRepository chatMessageRepository,
			UserRepository userRepository
	) {
		this.postReportRepository = postReportRepository;
		this.lostItemRepository = lostItemRepository;
		this.foundItemRepository = foundItemRepository;
		this.animalReportRepository = animalReportRepository;
		this.missingPersonRepository = missingPersonRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public String create(ReportCreateRequest request, User reporter) {
		if (reporter == null) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		validateCreateRequest(request);

		ReportTargetSnapshot target = resolveTarget(request.getTargetType(), request.getTargetId());
		boolean isChat = request.getTargetType() == ReportTargetType.CHAT_MESSAGE;
		String targetNoun = isChat ? "메시지" : "게시글";
		Long reporterNo = reporter.getUserNo();
		if (reporterNo != null && reporterNo.equals(target.ownerNo())) {
			throw new IllegalArgumentException(isChat ? "본인이 보낸 메시지는 신고할 수 없습니다." : "본인이 작성한 게시글은 신고할 수 없습니다.");
		}
		if (postReportRepository.existsByReporterUserNoAndTargetTypeAndTargetId(reporterNo, request.getTargetType(), request.getTargetId())) {
			throw new IllegalArgumentException("이미 신고한 " + targetNoun + "입니다.");
		}

		PostReport report = new PostReport(
				reporter,
				request.getTargetType(),
				request.getTargetId(),
				target.ownerNo(),
				target.title(),
				target.url(),
				request.getReasonType(),
				normalizeReasonDetail(request.getReasonDetail())
		);
		postReportRepository.save(report);
		return target.url();
	}

	@Transactional(readOnly = true)
	public Page<AdminReportRow> search(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType, Pageable pageable) {
		return postReportRepository.findAll(searchSpec(status, targetType, reasonType), pageable)
				.map(this::toAdminReportRow);
	}

	@Transactional
	public void updateStatus(Long reportId, ReportStatus status, String adminMemo, User handler) {
		if (status == null) {
			throw new IllegalArgumentException("처리 상태를 선택해 주세요.");
		}
		PostReport report = postReportRepository.findById(reportId)
				.orElseThrow(() -> new IllegalArgumentException("신고 내역을 찾을 수 없습니다."));
		report.updateStatus(status, trimToNull(adminMemo), handler);
	}

	public String fallbackTargetUrl(ReportTargetType targetType, Long targetId) {
		if (targetType == null || targetId == null) {
			return "/";
		}
		return switch (targetType) {
			case LOST_ITEM -> "/lost-items/" + targetId;
			case FOUND_ITEM -> "/found-items/" + targetId;
			case ANIMAL_REPORT -> "/animal-reports/" + targetId;
			case MISSING_PERSON -> "/missing-persons/" + targetId;
			case CHAT_MESSAGE -> "/chat";
		};
	}

	private Specification<PostReport> searchSpec(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType) {
		return (root, query, criteriaBuilder) -> {
			var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}
			if (targetType != null) {
				predicates.add(criteriaBuilder.equal(root.get("targetType"), targetType));
			}
			if (reasonType != null) {
				predicates.add(criteriaBuilder.equal(root.get("reasonType"), reasonType));
			}
			return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private AdminReportRow toAdminReportRow(PostReport report) {
		User reporter = report.getReporter();
		Long targetOwnerNo = report.getTargetOwnerNo();
		String targetOwnerNickname = "알 수 없음";
		String targetOwnerLoginId = "-";
		if (targetOwnerNo != null) {
			var ownerOpt = userRepository.findById(targetOwnerNo);
			if (ownerOpt.isPresent()) {
				var owner = ownerOpt.get();
				targetOwnerNickname = owner.getNickname();
				targetOwnerLoginId = owner.getLoginId();
			}
		}
		long targetOwnerReportCount = postReportRepository.countByTargetOwnerNoAndStatusNot(targetOwnerNo, ReportStatus.REJECTED);

		return new AdminReportRow(
				report.getReportId(),
				report.getStatus(),
				report.getTargetType(),
				report.getTargetTitle(),
				report.getTargetUrl(),
				report.getReasonType(),
				report.getReasonDetail(),
				reporter != null ? reporter.getUserNo() : null,
				reporter != null ? reporter.getNickname() : null,
				reporter != null ? reporter.getLoginId() : null,
				report.getCreatedAt(),
				report.getAdminMemo(),
				targetOwnerNo,
				targetOwnerNickname,
				targetOwnerLoginId,
				targetOwnerReportCount
		);
	}

	private void validateCreateRequest(ReportCreateRequest request) {
		if (request == null || request.getTargetType() == null || request.getTargetId() == null) {
			throw new IllegalArgumentException("신고 대상 정보가 올바르지 않습니다.");
		}
		if (request.getReasonType() == null) {
			throw new IllegalArgumentException("신고 사유를 선택해 주세요.");
		}
		String reasonDetail = request.getReasonDetail();
		if (reasonDetail != null && reasonDetail.trim().length() > MAX_REASON_DETAIL_LENGTH) {
			throw new IllegalArgumentException("상세 사유는 1000자 이하로 입력해 주세요.");
		}
	}

	private ReportTargetSnapshot resolveTarget(ReportTargetType targetType, Long targetId) {
		return switch (targetType) {
			case LOST_ITEM -> resolveLostItem(targetId);
			case FOUND_ITEM -> resolveFoundItem(targetId);
			case ANIMAL_REPORT -> resolveAnimalReport(targetId);
			case MISSING_PERSON -> resolveMissingPerson(targetId);
			case CHAT_MESSAGE -> resolveChatMessage(targetId);
		};
	}

	private ReportTargetSnapshot resolveChatMessage(Long targetId) {
		ChatMessage message = chatMessageRepository.findById(targetId)
				.orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));
		Long ownerNo = ownerNo(message.getSender());
		String senderName = firstText(message.getSender().getNickname(), "알 수 없음");
		String preview = senderName + ": " + firstText(message.getContent(), "(파일)");
		if (preview.length() > 300) {
			preview = preview.substring(0, 300);
		}
		String url = "/admin/reports/chat/" + message.getChatRoom().getRoomId();
		return new ReportTargetSnapshot(ownerNo, preview, url);
	}

	private ReportTargetSnapshot resolveLostItem(Long targetId) {
		LostItem item = lostItemRepository.findById(targetId)
				.filter(candidate -> !candidate.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
		assertUserSource(item.getSourceType());
		Long ownerNo = ownerNo(item.getUser());
		return new ReportTargetSnapshot(ownerNo, firstText(item.getTitle(), item.getItemName(), "분실물 신고"), "/lost-items/" + item.getLostId());
	}

	private ReportTargetSnapshot resolveFoundItem(Long targetId) {
		FoundItem item = foundItemRepository.findById(targetId)
				.filter(candidate -> !candidate.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
		assertUserSource(item.getSourceType());
		Long ownerNo = ownerNo(item.getUser());
		return new ReportTargetSnapshot(ownerNo, firstText(item.getTitle(), item.getItemName(), "습득물 신고"), "/found-items/" + item.getFoundId());
	}

	private ReportTargetSnapshot resolveAnimalReport(Long targetId) {
		AnimalReport report = animalReportRepository.findById(targetId)
				.filter(candidate -> !candidate.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
		assertUserSource(report.getSourceType());
		Long ownerNo = ownerNo(report.getUser());
		return new ReportTargetSnapshot(ownerNo, firstText(report.getTitle(), report.getBreedName(), "동물 신고"), "/animal-reports/" + report.getReportId());
	}

	private ReportTargetSnapshot resolveMissingPerson(Long targetId) {
		MissingPersonReport report = missingPersonRepository.findById(targetId)
				.filter(candidate -> !candidate.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
		assertUserSource(report.getSourceType());
		Long ownerNo = ownerNo(report.getUser());
		String title = firstText(report.getPersonName(), report.getAge() + "세 " + report.getNationality() + " 실종", "실종자 신고");
		return new ReportTargetSnapshot(ownerNo, title, "/missing-persons/" + report.getReportId());
	}

	private void assertUserSource(String sourceType) {
		if (!"USER".equals(sourceType)) {
			throw new IllegalArgumentException("사용자가 등록한 게시글만 신고할 수 있습니다.");
		}
	}

	private Long ownerNo(User user) {
		if (user == null || user.getUserNo() == null) {
			throw new IllegalArgumentException("사용자가 등록한 게시글만 신고할 수 있습니다.");
		}
		return user.getUserNo();
	}

	private String firstText(String... values) {
		for (String value : values) {
			if (StringUtils.hasText(value)) {
				return value.trim();
			}
		}
		return "";
	}

	private String normalizeReasonDetail(String reasonDetail) {
		String normalized = trimToNull(reasonDetail);
		return normalized == null ? null : normalized.substring(0, Math.min(normalized.length(), MAX_REASON_DETAIL_LENGTH));
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim().replaceAll("\\s+", " ");
	}

	private record ReportTargetSnapshot(Long ownerNo, String title, String url) {
	}
}
