package com.example.dogo.controller.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import com.example.dogo.service.missing.MissingAlertService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/missing-alerts")
public class MissingAlertApiController {

	private final MissingAlertService missingAlertService;

	public MissingAlertApiController(MissingAlertService missingAlertService) {
		this.missingAlertService = missingAlertService;
	}

	@GetMapping
	public Safe182AmberAlertPage alerts(
			@RequestParam(defaultValue = "10") int rowSize,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate occrde
	) {
		return missingAlertService.fetchAlerts(rowSize, page, occrde);
	}
}
