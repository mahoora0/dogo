package com.example.dogo.service.missing;

import com.example.dogo.dto.item.RecentItemView;
import com.example.dogo.dto.missing.MissingPersonCreateRequest;
import com.example.dogo.dto.missing.MissingPersonDetailView;
import com.example.dogo.dto.missing.MissingPersonView;
import com.example.dogo.entity.missing.MissingPersonImage;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.missing.MissingPersonImageRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MissingPersonService {

	private static final String DEV_USER_EMAIL = "dev@dogo.local";

	private final MissingPersonRepository missingPersonRepository;
	private final MissingPersonImageRepository missingPersonImageRepository;
	private final UserRepository userRepository;
	private final Path missingPersonUploadPath;

	public MissingPersonService(
			MissingPersonRepository missingPersonRepository,
			MissingPersonImageRepository missingPersonImageRepository,
			UserRepository userRepository,
			@Value("${file.upload-dir}") String uploadDir
	) {
		this.missingPersonRepository = missingPersonRepository;
		this.missingPersonImageRepository = missingPersonImageRepository;
		this.userRepository = userRepository;
		this.missingPersonUploadPath = Path.of(uploadDir, "missing-persons").toAbsolutePath().normalize();
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

	@Transactional(readOnly = true)
	public List<RecentItemView> getRecentItems(int limit) {
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "occurredAt").and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "reportId")));
		return missingPersonRepository.findAll(searchSpec(null, null, null), pageable).stream()
				.map(item -> new RecentItemView(
						item.getReportId(),
						"PERSON",
						"실종자",
						summary(item),
						"사람",
						item.getOccurredPlace(),
						item.getOccurredAt(),
						item.getStatus(),
						statusLabel(item.getStatus()),
						thumbnailImageUrl(item),
						item.getRegdate()
				))
				.toList();
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

		MissingPersonReport saved = missingPersonRepository.save(report);
		saveImages(saved, request.getUploadImages());
		return saved.getReportId();
	}

	private void saveImages(MissingPersonReport report, List<MultipartFile> images) {
		for (int index = 0; index < images.size(); index++) {
			saveImageIfPresent(report, images.get(index), index);
		}
	}

	private void saveImageIfPresent(MissingPersonReport report, MultipartFile image, int sortOrder) {
		if (image == null || image.isEmpty()) {
			return;
		}

		try {
			Files.createDirectories(missingPersonUploadPath);

			String originalName = StringUtils.cleanPath(String.valueOf(image.getOriginalFilename()));
			String extension = extractExtension(originalName);
			String storedName = UUID.randomUUID() + extension;
			Path targetPath = missingPersonUploadPath.resolve(storedName).normalize();
			if (!targetPath.startsWith(missingPersonUploadPath)) {
				throw new IllegalArgumentException("올바르지 않은 이미지 파일명입니다.");
			}

			image.transferTo(targetPath);

			missingPersonImageRepository.save(new MissingPersonImage(
					report,
					originalName,
					storedName,
					"/uploads/missing-persons/" + storedName,
					image.getContentType(),
					image.getSize(),
					sortOrder
			));
		} catch (IOException exception) {
			throw new UncheckedIOException("이미지 저장에 실패했습니다.", exception);
		}
	}

	private String extractExtension(String filename) {
		if (!StringUtils.hasText(filename) || !filename.contains(".")) {
			return "";
		}
		String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
		if (extension.length() > 12) {
			return "";
		}
		return extension;
	}

	private List<String> findImageUrls(MissingPersonReport report) {
		return missingPersonImageRepository.findByReportOrderBySortOrderAscImageIdAsc(report).stream()
				.map(MissingPersonImage::getImageUrl)
				.toList();
	}

	private String thumbnailImageUrl(MissingPersonReport report) {
		return missingPersonImageRepository.findFirstByReportOrderBySortOrderAscImageIdAsc(report)
				.map(MissingPersonImage::getImageUrl)
				.orElseGet(() -> extractBase64Image(report.getRawPayload()));
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
				predicates.add(criteriaBuilder.like(root.get("searchContent"), pattern));
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
				report.getSourceLabel(),
				extractBase64Image(report.getRawPayload()),
				findImageUrls(report)
		);
	}

	private MissingPersonDetailView toDetailView(MissingPersonReport report) {
		String raw = report.getRawPayload();
		
		Integer ageNow = null;
		String ageNowStr = extractXmlTag(raw, "ageNow");
		if (ageNowStr != null) {
			try {
				ageNow = Integer.parseInt(ageNowStr);
			} catch (Exception e) {
				// ignore
			}
		}
		
		String targetCode = extractXmlTag(raw, "writngTrgetDscd");
		String targetLabel = getTargetLabel(targetCode);
		
		String gender = extractXmlTag(raw, "sexdstnDscd");
		if (gender == null || gender.isBlank()) {
			gender = report.getGender();
		}

		String etcSpfeatr = extractXmlTag(raw, "etcSpfeatr");

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
				report.getSourceLabel(),
				extractBase64Image(report.getRawPayload()),
				ageNow,
				targetCode,
				targetLabel,
				gender,
				etcSpfeatr,
				findImageUrls(report)
		);
	}

	private String extractXmlTag(String rawPayload, String tagName) {
		if (rawPayload == null || !rawPayload.contains("<" + tagName + ">")) {
			return null;
		}
		try {
			int start = rawPayload.indexOf("<" + tagName + ">");
			int end = rawPayload.indexOf("</" + tagName + ">", start);
			if (start != -1 && end != -1) {
				String content = rawPayload.substring(start + tagName.length() + 2, end).trim();
				if (content.startsWith("<![CDATA[")) {
					content = content.substring("<![CDATA[".length(), content.length() - "]]>".length()).trim();
				}
				if (!content.isEmpty() && !content.equalsIgnoreCase("null")) {
					return content;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	private String getTargetLabel(String code) {
		if (code == null) return null;
		return switch (code.trim()) {
			case "010" -> "정상아동(18세미만)";
			case "020" -> "가출인";
			case "040" -> "시설보호무연고자";
			case "060" -> "지적장애인";
			case "061" -> "지적장애인(18세미만)";
			case "062" -> "지적장애인(18세이상)";
			case "070" -> "치매질환자";
			case "080" -> "불상(기타)";
			default -> "기타 (" + code + ")";
		};
	}

	private String extractBase64Image(String rawPayload) {
		if (rawPayload == null || !rawPayload.contains("<tknphotoFile>")) {
			return null;
		}
		try {
			int start = rawPayload.indexOf("<tknphotoFile>");
			int end = rawPayload.indexOf("</tknphotoFile>", start);
			if (start != -1 && end != -1) {
				String content = rawPayload.substring(start + "<tknphotoFile>".length(), end).trim();
				if (content.startsWith("<![CDATA[")) {
					content = content.substring("<![CDATA[".length(), content.length() - "]]>".length()).trim();
				}
				if (!content.isEmpty() && !content.equalsIgnoreCase("null")) {
					return "data:image/jpeg;base64," + content;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
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
