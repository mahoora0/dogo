package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import com.example.dogo.dto.missing.Safe182AmberAlertView;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MissingAlertService {

	private final MissingPersonRepository missingPersonRepository;
	private final Safe182AmberAlertClient safe182AmberAlertClient;

	public MissingAlertService(MissingPersonRepository missingPersonRepository, Safe182AmberAlertClient safe182AmberAlertClient) {
		this.missingPersonRepository = missingPersonRepository;
		this.safe182AmberAlertClient = safe182AmberAlertClient;
	}

	public Safe182AmberAlertPage fetchAlerts(int rowSize, Integer page, LocalDate occurrenceDate) {
		try {
			LocalDate dateToFetch = (occurrenceDate != null) ? occurrenceDate : LocalDate.now();
			return safe182AmberAlertClient.fetchAlerts(dateToFetch, rowSize, page);
		} catch (Exception exception) {
			// Fallback to local DB if API call fails (network issue, credential issue, limit exceeded, etc.)
			int safeRowSize = Math.max(1, Math.min(rowSize, 100));
			int safePage = (page == null || page < 1) ? 0 : page - 1;

			Page<MissingPersonReport> reportPage = missingPersonRepository.findBySourceTypeAndDeletedFalse(
					"PUBLIC_API",
					PageRequest.of(safePage, safeRowSize, Sort.by(Sort.Direction.DESC, "regdate").and(Sort.by(Sort.Direction.DESC, "reportId")))
			);

			List<Safe182AmberAlertView> alerts = reportPage.getContent().stream()
					.map(this::toAmberAlertView)
					.toList();

			return new Safe182AmberAlertPage("00", "OK", (int) reportPage.getTotalElements(), alerts);
		}
	}

	private Safe182AmberAlertView toAmberAlertView(MissingPersonReport report) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
		String occurrenceDate = report.getOccurredAt() != null ? report.getOccurredAt().format(formatter) : "";
		
		return new Safe182AmberAlertView(
				occurrenceDate,
				report.getClothing(),
				report.getAge() != null ? String.valueOf(report.getAge()) : "",
				report.getAge() != null ? String.valueOf(report.getAge()) : "",
				"PUBLIC_API".equals(report.getSourceType()) ? "실종 경보" : "사용자 제보",
				report.getGender(),
				report.getOccurredPlace(),
				report.getPersonName(),
				report.getHeightCm() != null ? String.valueOf(report.getHeightCm()) : "",
				report.getWeightKg() != null ? String.valueOf(report.getWeightKg()) : "",
				report.getBodyType(),
				report.getFaceShape(),
				report.getHairStyle(),
				report.getHairColor(),
				"",
				report.getSourceLabel()
		);
	}
}
