package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;

import java.time.LocalDate;

public interface Safe182AmberAlertClient {

	Safe182AmberAlertPage fetchAlerts(LocalDate occurrenceDate, int rowSize, Integer page);
}
