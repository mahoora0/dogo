package com.example.dogo.service.animal;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportMatch;
import com.example.dogo.repository.animal.AnimalReportMatchRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalMatchServiceTest {

	@Mock
	private AnimalReportRepository reportRepository;

	@Mock
	private AnimalReportMatchRepository matchRepository;

	@Mock
	private AnimalImageEmbeddingService embeddingService;

	private AnimalMatchService matchService;

	@BeforeEach
	void setUp() {
		matchService = new AnimalMatchService(reportRepository, matchRepository, embeddingService);
	}

	@Test
	void matchForReport_keepsExistingMatchesWhenQueryVectorIsMissing() {
		AnimalReport missing = report(1L, "MISSING");
		AnimalReport sighting = report(2L, "SIGHTING");
		when(reportRepository.findSightingCandidates("DOG", missing.getEventDate().minusDays(60), missing.getEventDate().plusDays(60)))
				.thenReturn(List.of(sighting));
		when(embeddingService.loadVectors(List.of(1L))).thenReturn(Map.of());

		matchService.matchForReport(missing);

		verify(matchRepository, never()).deleteByMissingReport_ReportId(1L);
		verify(matchRepository, never()).deleteBySightingReport_ReportId(1L);
		verify(matchRepository, never()).saveAll(anyList());
		verify(matchRepository, never()).flush();
	}

	@Test
	void matchForReport_replacesMatchesAfterSuccessfulScoring() {
		AnimalReport missing = report(1L, "MISSING");
		AnimalReport sighting = report(2L, "TRANSFERRED");
		when(reportRepository.findSightingCandidates("DOG", missing.getEventDate().minusDays(60), missing.getEventDate().plusDays(60)))
				.thenReturn(List.of(sighting));
		when(embeddingService.loadVectors(List.of(1L))).thenReturn(Map.of(1L, new float[] {1.0f, 0.0f}));
		when(embeddingService.loadVectors(List.of(2L))).thenReturn(Map.of(2L, new float[] {1.0f, 0.0f}));
		when(embeddingService.currentModelName()).thenReturn("test-model");

		matchService.matchForReport(missing);

		ArgumentCaptor<List<AnimalReportMatch>> captor = ArgumentCaptor.forClass(List.class);
		verify(matchRepository).deleteByMissingReport_ReportId(1L);
		verify(matchRepository).saveAll(captor.capture());
		verify(matchRepository).flush();

		List<AnimalReportMatch> saved = captor.getValue();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).getMissingReport()).isSameAs(missing);
		assertThat(saved.get(0).getSightingReport()).isSameAs(sighting);
		assertThat(saved.get(0).getFinalScore()).isEqualByComparingTo("100.00");
	}

	private AnimalReport report(Long id, String reportType) {
		AnimalReport report = new AnimalReport(
				new User(reportType.toLowerCase() + "@dogo.local", "신고자", "010-0000-0000"),
				reportType,
				reportType + " 신고",
				LocalDate.of(2026, 5, 10),
				null,
				null,
				"서울특별시",
				"강남역",
				null,
				true,
				null,
				"DOG",
				"말티즈",
				"UNKNOWN",
				"UNKNOWN",
				null,
				null,
				null,
				"흰색",
				null,
				null
		);
		ReflectionTestUtils.setField(report, "reportId", id);
		return report;
	}
}
