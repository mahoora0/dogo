package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.MissingPersonCreateRequest;
import com.example.dogo.dto.missing.MissingPersonDetailView;
import com.example.dogo.dto.missing.MissingPersonView;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class MissingPersonService {

	private static final String DEV_USER_EMAIL = "dev@dogo.local";

	private final MissingPersonRepository missingPersonRepository;
	private final UserRepository userRepository;

	public MissingPersonService(MissingPersonRepository missingPersonRepository, UserRepository userRepository) {
		this.missingPersonRepository = missingPersonRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Page<MissingPersonView> search(String keyword, String status, String sourceType, Pageable pageable) {
		return missingPersonRepository.findAll(searchSpec(keyword, status, sourceType), pageable)
				.map(this::toListView);
	}

	@Transactional(readOnly = true)
	public Page<MissingPersonView> search(String keyword, String status, Pageable pageable) {
		return search(keyword, status, null, pageable);
	}

	@Transactional
	public Long create(MissingPersonCreateRequest request, User loginUser) {
		validateCreateRequest(request);

		User user = (loginUser != null) ? loginUser : getOrCreateDevUser();
		MissingPersonReport report = new MissingPersonReport(
				user,
				request.getAge(),
				request.getNationality().trim(),
				request.getOccurredAt(),
				request.getOccurredPlace().trim(),
				request.getHeightCm(),
				request.getWeightKg(),
				request.getBodyType().trim(),
				request.getFaceShape().trim(),
				request.getHairColor().trim(),
				request.getHairStyle().trim(),
				request.getClothing().trim()
		);

		return missingPersonRepository.save(report).getReportId();
	}

	@Transactional(readOnly = true)
	public MissingPersonDetailView getDetail(Long id) {
		MissingPersonReport report = missingPersonRepository.findById(id)
				.filter(candidate -> !candidate.isDeleted())
				.orElseThrow(() -> new IllegalArgumentException("실종자 정보를 찾을 수 없습니다."));
		return toDetailView(report);
	}

	private Specification<MissingPersonReport> searchSpec(String keyword, String status, String sourceType) {
		return (root, query, criteriaBuilder) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
			predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

			String normalizedStatus = blankToNull(status);
			if (normalizedStatus != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), normalizedStatus));
			}

			String normalizedSourceType = blankToNull(sourceType);
			if (normalizedSourceType != null) {
				predicates.add(criteriaBuilder.equal(root.get("sourceType"), normalizedSourceType));
			}

			String normalizedKeyword = blankToNull(keyword);
			if (normalizedKeyword != null) {
				String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("personName")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("gender")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("nationality")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("occurredPlace")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("bodyType")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("faceShape")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("hairColor")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("hairStyle")), pattern),
						criteriaBuilder.like(criteriaBuilder.lower(root.get("clothing")), pattern)
				));
			}

			return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private MissingPersonView toListView(MissingPersonReport report) {
		return new MissingPersonView(
				report.getReportId(),
				summary(report),
				report.getAge(),
				report.getNationality(),
				report.getOccurredAt(),
				report.getOccurredPlace(),
				report.getHeightCm(),
				report.getWeightKg(),
				report.getBodyType(),
				report.getFaceShape(),
				report.getHairColor(),
				report.getHairStyle(),
				report.getClothing(),
				report.getStatus(),
				statusLabel(report.getStatus()),
				report.getSourceType(),
				report.getSourceLabel()
		);
	}

	private MissingPersonDetailView toDetailView(MissingPersonReport report) {
		return new MissingPersonDetailView(
				report.getReportId(),
				summary(report),
				report.getAge(),
				report.getNationality(),
				report.getOccurredAt(),
				report.getOccurredPlace(),
				report.getHeightCm(),
				report.getWeightKg(),
				report.getBodyType(),
				report.getFaceShape(),
				report.getHairColor(),
				report.getHairStyle(),
				report.getClothing(),
				report.getStatus(),
				statusLabel(report.getStatus()),
				report.getSourceType(),
				report.getSourceLabel()
		);
	}

	private void validateCreateRequest(MissingPersonCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("실종자 제보 정보를 입력해주세요.");
		}
		if (request.getAge() == null || request.getAge() < 0) {
			throw new IllegalArgumentException("나이를 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getNationality())) {
			throw new IllegalArgumentException("국적을 입력해주세요.");
		}
		if (request.getOccurredAt() == null) {
			throw new IllegalArgumentException("발생일시를 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getOccurredPlace())) {
			throw new IllegalArgumentException("발생장소를 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getBodyType())) {
			throw new IllegalArgumentException("체격을 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getFaceShape())) {
			throw new IllegalArgumentException("얼굴형을 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getHairColor())) {
			throw new IllegalArgumentException("머리색상을 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getHairStyle())) {
			throw new IllegalArgumentException("머리형태를 입력해주세요.");
		}
		if (!StringUtils.hasText(request.getClothing())) {
			throw new IllegalArgumentException("착의사항을 입력해주세요.");
		}
	}

	private User getOrCreateDevUser() {
		return userRepository.findByEmail(DEV_USER_EMAIL)
				.orElseGet(() -> userRepository.save(new User(DEV_USER_EMAIL, "개발자 사용자", "010-0000-0000")));
	}

	private String summary(MissingPersonReport report) {
		if (StringUtils.hasText(report.getPersonName())) {
			return report.getPersonName() + " (" + report.getAge() + "세)";
		}
		return report.getAge() + "세 " + report.getNationality() + " 실종";
	}

	private String statusLabel(String status) {
		return switch (status) {
			case "FOUND" -> "발견";
			case "CLOSED" -> "종료";
			default -> "접수";
		};
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
