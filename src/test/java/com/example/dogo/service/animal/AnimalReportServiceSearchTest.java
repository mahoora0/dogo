package com.example.dogo.service.animal;

import com.example.dogo.dto.animal.AnimalReportView;
import com.example.dogo.dto.animal.AnimalReportDetailView;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AnimalReportServiceSearchTest {

	@Autowired
	private AnimalReportService animalReportService;

	@Autowired
	private AnimalReportRepository animalReportRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void searchAppliesKeywordOnlyToSelectedScope() {
		User user = userRepository.save(new User("animal-search-test@dogo.local", "검색테스트", "010-0000-0000"));
		animalReportRepository.save(report(user, "강남에서 목격", "말티즈", "빨간 목줄", "한강공원"));
		animalReportRepository.save(report(user, "말티즈라는 단어가 있는 제목", "푸들", "파란 목줄", "서울숲"));

		Page<AnimalReportView> breedFeatureResult = animalReportService.search(
				null,
				null,
				null,
				"말티즈",
				"BREED_FEATURE",
				PageRequest.of(0, 10)
		);
		Page<AnimalReportView> titlePlaceResult = animalReportService.search(
				null,
				null,
				null,
				"말티즈",
				"TITLE_PLACE",
				PageRequest.of(0, 10)
		);
		Page<AnimalReportView> allResult = animalReportService.search(
				null,
				null,
				null,
				"말티즈",
				"ALL",
				PageRequest.of(0, 10)
		);

		assertThat(breedFeatureResult.getTotalElements()).isEqualTo(1);
		assertThat(breedFeatureResult.getContent().get(0).breedName()).isEqualTo("말티즈");
		assertThat(titlePlaceResult.getTotalElements()).isEqualTo(1);
		assertThat(titlePlaceResult.getContent().get(0).title()).isEqualTo("말티즈라는 단어가 있는 제목");
		assertThat(allResult.getTotalElements()).isEqualTo(2);
	}

	@Test
	void searchReturnsPublicApiReports() {
		AnimalReport publicReport = AnimalReport.fromPublicApi(
				"ANIMAL_LOSS_API",
				"loss-1",
				"MISSING",
				"Public API dog",
				LocalDate.of(2026, 5, 16),
				null,
				null,
				"Seoul",
				"Mapo",
				"010-1111-2222",
				true,
				null,
				"DOG",
				"Poodle",
				"FEMALE",
				"UNKNOWN",
				null,
				null,
				null,
				"white",
				"blue collar",
				"public animal loss record",
				"<item><id>loss-1</id></item>"
		);
		animalReportRepository.save(publicReport);

		Page<AnimalReportView> result = animalReportService.search(
				"MISSING",
				"DOG",
				null,
				"Poodle",
				"BREED_FEATURE",
				PageRequest.of(0, 10)
		);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).title()).isEqualTo("Public API dog");
		assertThat(publicReport.getSourceType()).isEqualTo("ANIMAL_LOSS_API");
		assertThat(publicReport.getApiProvider()).isEqualTo("ANIMAL_LOSS_API");
		assertThat(publicReport.getExternalId()).isEqualTo("loss-1");
		assertThat(publicReport.getSyncedAt()).isNotNull();
	}

	@Test
	void detailSeparatesPublicProtectionInfoFromDescription() {
		AnimalReport publicReport = AnimalReport.fromPublicApi(
				"ANIMAL_PROTECTION_API",
				"protect-1",
				"SIGHTING",
				"한국 고양이 구조 신고",
				LocalDate.of(2026, 6, 5),
				null,
				null,
				"부산광역시 연제구 온천천남로 4 (연산동)",
				"중앙대로1188",
				"051-503-0688",
				true,
				"PROTECTING",
				"CAT",
				"한국 고양이",
				"FEMALE",
				"NOT_NEUTERED",
				null,
				null,
				new java.math.BigDecimal("0.5"),
				"기타(흰색검은색)",
				"양호(온순)",
				"상태: 보호중\n보호소: 청조동물병원\n보호소 주소: 부산광역시 연제구 온천천남로 4 (연산동)\n공고번호: 부산-연제-2026-00069\n특징: 양호(온순)",
				"""
						<item>
						  <careNm>청조동물병원</careNm>
						  <careAddr>부산광역시 연제구 온천천남로 4 (연산동)</careAddr>
						  <orgNm>부산광역시 연제구</orgNm>
						  <noticeNo>부산-연제-2026-00069</noticeNo>
						  <noticeSdt>20260605</noticeSdt>
						  <noticeEdt>20260615</noticeEdt>
						  <age>2026(년생)</age>
						</item>
						"""
		);
		AnimalReport saved = animalReportRepository.save(publicReport);

		AnimalReportDetailView detail = animalReportService.getDetail(saved.getReportId());

		assertThat(detail.locationSummary()).isEqualTo("부산광역시 연제구 · 중앙대로1188");
		assertThat(detail.displayContent()).isNull();
		assertThat(detail.ageDisplay()).isEqualTo("2026(년생)");
		assertThat(detail.shelterName()).isEqualTo("청조동물병원");
		assertThat(detail.shelterAddress()).isEqualTo("부산광역시 연제구 온천천남로 4 (연산동)");
		assertThat(detail.authorityName()).isEqualTo("부산광역시 연제구");
		assertThat(detail.noticeNo()).isEqualTo("부산-연제-2026-00069");
		assertThat(detail.noticePeriod()).isEqualTo("2026-06-05 ~ 2026-06-15");
	}

	private AnimalReport report(
			User user,
			String title,
			String breedName,
			String distinctiveMarks,
			String detailPlace
	) {
		return new AnimalReport(
				user,
				"MISSING",
				title,
				LocalDate.of(2026, 5, 16),
				null,
				null,
				"서울특별시",
				detailPlace,
				null,
				true,
				null,
				"DOG",
				breedName,
				"UNKNOWN",
				"UNKNOWN",
				null,
				null,
				null,
				"흰색",
				distinctiveMarks,
				"상세 내용"
		);
	}

	@Test
	void searchSightingMatchesSightingAndProtecting() {
		User user = userRepository.save(new User("sighting-search-test@dogo.local", "목격검색테스트", "010-0000-0000"));
		
		AnimalReport sighting = new AnimalReport(user, "SIGHTING", "목격 게시글", LocalDate.of(2026, 5, 16), null, null, "서울특별시", "강남역", null, true, null, "DOG", "말티즈", "UNKNOWN", "UNKNOWN", null, null, null, "흰색", null, null);
		AnimalReport protecting = new AnimalReport(user, "PROTECTING", "보호 게시글", LocalDate.of(2026, 5, 16), null, null, "서울특별시", "강남역", null, true, null, "DOG", "말티즈", "UNKNOWN", "UNKNOWN", null, null, null, "흰색", null, null);
		AnimalReport transferred = new AnimalReport(user, "TRANSFERRED", "연계 게시글", LocalDate.of(2026, 5, 16), null, null, "서울특별시", "강남역", null, true, null, "DOG", "말티즈", "UNKNOWN", "UNKNOWN", null, null, null, "흰색", null, null);

		animalReportRepository.save(sighting);
		animalReportRepository.save(protecting);
		animalReportRepository.save(transferred);

		Page<AnimalReportView> sightingResult = animalReportService.search("SIGHTING", null, null, null, "ALL", PageRequest.of(0, 10));
		Page<AnimalReportView> protectingResult = animalReportService.search("PROTECTING", null, null, null, "ALL", PageRequest.of(0, 10));

		// SIGHTING filter should match SIGHTING and PROTECTING reports, but not TRANSFERRED
		assertThat(sightingResult.getContent()).extracting(AnimalReportView::reportType)
				.containsExactlyInAnyOrder("SIGHTING", "PROTECTING");

		// PROTECTING filter should strictly match PROTECTING reports
		assertThat(protectingResult.getContent()).extracting(AnimalReportView::reportType)
				.containsExactly("PROTECTING");
	}
}
