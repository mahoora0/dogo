package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiPage;
import com.example.dogo.dto.animal.AnimalPublicApiRecord;
import com.example.dogo.dto.animal.AnimalPublicApiSyncResult;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.repository.animal.AnimalReportImageRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AnimalPublicApiSyncServiceTest {

	@Autowired
	private AnimalReportRepository animalReportRepository;

	@Autowired
	private AnimalReportImageRepository animalReportImageRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Test
	void syncLossRecordsSavesMissingReportsAndSkipsDuplicates() {
		AnimalPublicApiRecord record = new AnimalPublicApiRecord(
				"LOSS-1",
				"20260515",
				"Gangnam station",
				"Seoul Gangnam-gu",
				null,
				null,
				"02-111-2222",
				"Dog",
				"Poodle",
				"White",
				"F",
				"N",
				null,
				null,
				"Blue collar",
				null,
				null,
				null,
				null,
				"https://example.test/loss.jpg",
				"<item><lossNo>LOSS-1</lossNo></item>"
		);
		AnimalPublicApiSyncService service = service(new FakeClient(record), "ANIMAL_LOSS_API", "MISSING");

		AnimalPublicApiSyncResult first = service.sync(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 22));
		AnimalPublicApiSyncResult second = service.sync(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 22));

		assertThat(first.savedCount()).isEqualTo(1);
		assertThat(second.skippedCount()).isEqualTo(1);
		AnimalReport saved = animalReportRepository.findByApiProviderAndExternalId("ANIMAL_LOSS_API", "LOSS-1").orElseThrow();
		assertThat(saved.getReportType()).isEqualTo("MISSING");
		assertThat(saved.getSourceType()).isEqualTo("ANIMAL_LOSS_API");
		assertThat(saved.getUser()).isNull();
		assertThat(animalReportImageRepository.findByAnimalReportOrderBySortOrderAscImageIdAsc(saved))
				.extracting("imageUrl")
				.containsExactly("https://example.test/loss.jpg");
	}

	@Test
	void syncProtectionRecordsSavesSightingReports() {
		AnimalPublicApiRecord record = new AnimalPublicApiRecord(
				"PROTECT-1",
				"20260516",
				"Mapo shelter road",
				"Seoul Mapo-gu",
				"Mapo Shelter",
				"Seoul Mapo-gu",
				"02-333-4444",
				"[Dog] Maltese",
				null,
				"Cream",
				"M",
				"Y",
				null,
				null,
				"Friendly",
				"Protecting",
				"NOTICE-1",
				"20260516",
				"20260526",
				null,
				"<item><desertionNo>PROTECT-1</desertionNo></item>"
		);
		AnimalPublicApiSyncService service = service(new FakeClient(record), "ANIMAL_PROTECTION_API", "SIGHTING");

		AnimalPublicApiSyncResult result = service.sync(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 22));

		assertThat(result.savedCount()).isEqualTo(1);
		AnimalReport saved = animalReportRepository.findByApiProviderAndExternalId("ANIMAL_PROTECTION_API", "PROTECT-1").orElseThrow();
		assertThat(saved.getReportType()).isEqualTo("SIGHTING");
		assertThat(saved.getSightingCareStatus()).isEqualTo("PROTECTING");
		assertThat(saved.getBreedName()).isEqualTo("Maltese");
		assertThat(saved.getAnimalType()).isEqualTo("DOG");
	}

	private AnimalPublicApiSyncService service(AnimalPublicApiClient client, String sourceType, String reportType) {
		return new AnimalPublicApiSyncService(
				client,
				new AnimalPublicApiMapper(),
				new AnimalPublicApiImageService(animalReportImageRepository),
				animalReportRepository,
				eventPublisher,
				sourceType,
				reportType,
				100,
				1,
				7
		);
	}

	private record FakeClient(AnimalPublicApiRecord record) implements AnimalPublicApiClient {
		@Override
		public AnimalPublicApiPage fetch(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows) {
			if (pageNo > 1) {
				return new AnimalPublicApiPage("00", "OK", 1, List.of());
			}
			return new AnimalPublicApiPage("00", "OK", 1, List.of(record));
		}
	}
}
