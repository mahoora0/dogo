package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.MissingPersonCreateRequest;
import com.example.dogo.entity.missing.MissingPersonImage;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.missing.MissingPersonImageRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.service.missing.sync.Safe182MissingPersonSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissingPersonServiceTest {

	private final MissingPersonRepository missingPersonRepository = mock(MissingPersonRepository.class);
	private final MissingPersonImageRepository missingPersonImageRepository = mock(MissingPersonImageRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final Safe182MissingPersonSyncService safe182MissingPersonSyncService = mock(Safe182MissingPersonSyncService.class);
	private final MissingPersonService missingPersonService = new MissingPersonService(
			missingPersonRepository,
			missingPersonImageRepository,
			userRepository,
			safe182MissingPersonSyncService,
			"build/test-uploads"
	);

	@Test
	void createStoresRequiredMissingPersonFields() {
		MissingPersonCreateRequest request = createRequest();
		User user = new User("login@dogo.local", "로그인 사용자", "010-0000-0000");
		when(missingPersonRepository.save(any(MissingPersonReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

		missingPersonService.create(request, user);

		verify(missingPersonRepository).save(any(MissingPersonReport.class));
	}

	@Test
	void userCreatedReportDefaultsToUserSource() {
		MissingPersonReport report = createReport();

		assertThat(report.getSourceType()).isEqualTo("USER");
		assertThat(report.getSourceLabel()).isEqualTo("사용자 제보");
		assertThat(report.getExternalId()).isNull();
	}

	@Test
	void publicApiReportStoresImportMetadata() {
		MissingPersonReport report = MissingPersonReport.fromPublicApi(
				"MISSING_ALERT",
				"case-20260518-001",
				"{\"id\":\"case-20260518-001\"}",
				"Hong Gil-dong",
				"male",
				13,
				"Korea",
				LocalDateTime.of(2026, 5, 18, 9, 30),
				"Seoul",
				170,
				new BigDecimal("58.0"),
				"Slim",
				"Oval",
				"Black",
				"Short",
				"Blue hoodie"
		);

		assertThat(report.getSourceType()).isEqualTo("PUBLIC_API");
		assertThat(report.getSourceLabel()).isEqualTo("데이터 출처: 경찰청");
		assertThat(report.getApiProvider()).isEqualTo("MISSING_ALERT");
		assertThat(report.getExternalId()).isEqualTo("case-20260518-001");
		assertThat(report.getPersonName()).isEqualTo("Hong Gil-dong");
		assertThat(report.getGender()).isEqualTo("male");
		assertThat(report.getRawPayload()).contains("case-20260518-001");
		assertThat(report.getSyncedAt()).isNotNull();
	}

	@Test
	void createRejectsMissingRequiredFields() {
		MissingPersonCreateRequest request = createRequest();
		request.setOccurredPlace(" ");

		assertThatThrownBy(() -> missingPersonService.create(request, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("발생장소");
	}

	@Test
	void searchReturnsListViews() {
		when(missingPersonRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(createReport())));

		var result = missingPersonService.search("Korea", "OPEN", null, PageRequest.of(0, 9));

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).summary()).isEqualTo("13세 Korea 실종");
		assertThat(result.getContent().get(0).statusLabel()).isEqualTo("실종");
		assertThat(result.getContent().get(0).sourceType()).isEqualTo("USER");
		assertThat(result.getContent().get(0).sourceLabel()).isEqualTo("사용자 제보");
	}

	@Test
	void searchAcceptsSourceTypeFilter() {
		when(missingPersonRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of()));

		missingPersonService.search("Seoul", "OPEN", "PUBLIC_API", PageRequest.of(0, 9));

		verify(missingPersonRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
	}

	@Test
	void searchAcceptsNewFilters() {
		when(missingPersonRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of()));

		missingPersonService.search("Seoul", "OPEN", "PUBLIC_API", "서울특별시", java.time.LocalDate.now(), "강남역", PageRequest.of(0, 9));

		verify(missingPersonRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
	}

	@Test
	void searchTriggersApiSyncWhenKeywordIsPresent() {
		when(missingPersonRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of()));

		missingPersonService.search("Seoul", "OPEN", "PUBLIC_API", PageRequest.of(0, 9));

		verify(safe182MissingPersonSyncService).syncSearch("Seoul");
	}

	@Test
	void recentItemsUseUploadedMissingPersonImageFirst() {
		MissingPersonReport report = createReport();
		when(missingPersonRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(report)));
		when(missingPersonImageRepository.findByReportInOrderBySortOrderAscImageIdAsc(List.of(report)))
				.thenReturn(List.of(new MissingPersonImage(
						report,
						"person.png",
						"stored.png",
						"/uploads/missing-persons/stored.png",
						"image/png",
						123L,
						0
				)));

		var result = missingPersonService.getRecentItems(5);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).imageUrl()).isEqualTo("/uploads/missing-persons/stored.png");
	}

	private MissingPersonReport createReport() {
		return new MissingPersonReport(
				new User("dev@dogo.local", "dev", "010-0000-0000"),
				13,
				"Korea",
				LocalDateTime.of(2026, 5, 18, 9, 30),
				"Seoul",
				170,
				new BigDecimal("58.0"),
				"Slim",
				"Oval",
				"Black",
				"Short",
				"Blue hoodie"
		);
	}

	private MissingPersonCreateRequest createRequest() {
		MissingPersonCreateRequest request = new MissingPersonCreateRequest();
		request.setAge(13);
		request.setNationality("Korea");
		request.setOccurredAt(LocalDateTime.of(2026, 5, 18, 9, 30));
		request.setOccurredPlace("Seoul");
		request.setHeightCm(170);
		request.setWeightKg(new BigDecimal("58.0"));
		request.setBodyType("Slim");
		request.setFaceShape("Oval");
		request.setHairColor("Black");
		request.setHairStyle("Short");
		request.setClothing("Blue hoodie");
		return request;
	}
}
