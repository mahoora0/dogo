package com.example.dogo.service.animal;

import com.example.dogo.dto.animal.AnimalReportView;
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
}
