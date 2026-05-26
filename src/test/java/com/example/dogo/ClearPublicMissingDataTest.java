package com.example.dogo;

import com.example.dogo.config.MissingPersonSearchBackfiller;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ClearPublicMissingDataTest {

	@Autowired
	private MissingPersonRepository missingPersonRepository;

	@Autowired
	private MissingPersonSearchBackfiller backfiller;

	@Test
	void cleansUpCorruptedDataOnBackfill() {
		// Given: A report with corrupted height and literal "null" text fields
		MissingPersonReport corruptedReport = MissingPersonReport.fromPublicApi(
				"TEST_API",
				"test-external-id-123",
				"{}",
				"Test Name",
				"male",
				50,
				"대한민국",
				LocalDateTime.now(),
				"null",      // occurredPlace
				27420,       // heightCm
				new BigDecimal("70.0"),
				"null",      // bodyType
				"null",      // faceShape
				"null",      // hairColor
				"null",      // hairStyle
				"null"       // clothing
		);

		missingPersonRepository.save(corruptedReport);

		// When: We run the backfiller
		backfiller.backfill();

		// Then: The report in the repository should be cleaned up
		var cleanedReport = missingPersonRepository.findByApiProviderAndExternalId("TEST_API", "test-external-id-123").orElseThrow();
		assertThat(cleanedReport.getHeightCm()).isNull();
		assertThat(cleanedReport.getClothing()).isEqualTo("착의 정보 없음");
		assertThat(cleanedReport.getBodyType()).isEqualTo("미상");
		assertThat(cleanedReport.getFaceShape()).isEqualTo("미상");
		assertThat(cleanedReport.getHairColor()).isEqualTo("미상");
		assertThat(cleanedReport.getHairStyle()).isEqualTo("미상");
		assertThat(cleanedReport.getOccurredPlace()).isEqualTo("장소 미상");

		// Clean up after test
		missingPersonRepository.delete(cleanedReport);
	}

	@Test
	void inspectDatabase() {
		long total = missingPersonRepository.count();
		long nonNullHeights = missingPersonRepository.findAll().stream()
				.filter(r -> r.getHeightCm() != null)
				.count();
		long nonNullWeights = missingPersonRepository.findAll().stream()
				.filter(r -> r.getWeightKg() != null)
				.count();
		System.out.println("=== DB INSPECTION ===");
		System.out.println("TOTAL RECORDS: " + total);
		System.out.println("WITH HEIGHT: " + nonNullHeights);
		System.out.println("WITH WEIGHT: " + nonNullWeights);
		System.out.println("=====================");
		
		// Let's print some records with height
		missingPersonRepository.findAll().stream()
				.filter(r -> r.getHeightCm() != null)
				.limit(5)
				.forEach(r -> {
					System.out.println("Name: " + r.getPersonName() + ", Height: " + r.getHeightCm() + ", Weight: " + r.getWeightKg());
				});
	}
}
