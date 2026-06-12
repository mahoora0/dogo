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

	public MissingAlertService(MissingPersonRepository missingPersonRepository) {
		this.missingPersonRepository = missingPersonRepository;
	}

	public Safe182AmberAlertPage fetchAlerts(int rowSize, Integer page, LocalDate occurrenceDate) {
		// Fetch only active missing persons (deleted = false, status = "OPEN") limited to 3 items
		int limitSize = 3;
		Page<MissingPersonReport> reportPage = missingPersonRepository.findAll(
				(root, query, cb) -> cb.and(
						cb.isFalse(root.get("deleted")),
						cb.equal(root.get("status"), "OPEN")
				),
				PageRequest.of(0, limitSize, Sort.by(Sort.Direction.DESC, "regdate").and(Sort.by(Sort.Direction.DESC, "reportId")))
		);

		List<Safe182AmberAlertView> alerts = reportPage.getContent().stream()
				.map(this::toAmberAlertView)
				.toList();

		return new Safe182AmberAlertPage("00", "OK", (int) reportPage.getTotalElements(), alerts);
	}

	private Safe182AmberAlertView toAmberAlertView(MissingPersonReport report) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
		String occurrenceDate = report.getOccurredAt() != null ? report.getOccurredAt().format(formatter) : "";
		
		return new Safe182AmberAlertView(
				report.getReportId(),
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
