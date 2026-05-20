package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MissingAlertService {

	private final Safe182AmberAlertClient safe182AmberAlertClient;

	public MissingAlertService(Safe182AmberAlertClient safe182AmberAlertClient) {
		this.safe182AmberAlertClient = safe182AmberAlertClient;
	}

	public Safe182AmberAlertPage fetchAlerts(int rowSize, Integer page, LocalDate occurrenceDate) {
		int safeRowSize = Math.max(1, Math.min(rowSize, 100));
		Integer safePage = page == null || page < 1 ? null : page;
		LocalDate safeOccurrenceDate = occurrenceDate == null ? LocalDate.now() : occurrenceDate;
		return safe182AmberAlertClient.fetchAlerts(safeOccurrenceDate, safeRowSize, safePage);
	}
}
