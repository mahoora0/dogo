package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiPage;
import com.example.dogo.dto.animal.AnimalPublicApiRecord;
import com.example.dogo.dto.animal.AnimalPublicApiSyncResult;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.service.animal.AnimalReportCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public class AnimalPublicApiSyncService {

	private static final Logger log = LoggerFactory.getLogger(AnimalPublicApiSyncService.class);

	private final AnimalPublicApiClient client;
	private final AnimalPublicApiMapper mapper;
	private final AnimalPublicApiImageService imageService;
	private final AnimalReportRepository animalReportRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final String sourceType;
	private final String reportType;
	private final int numOfRows;
	private final int incrementalLookbackDays;
	private final int backfillLookbackDays;

	public AnimalPublicApiSyncService(
			AnimalPublicApiClient client,
			AnimalPublicApiMapper mapper,
			AnimalPublicApiImageService imageService,
			AnimalReportRepository animalReportRepository,
			ApplicationEventPublisher eventPublisher,
			String sourceType,
			String reportType,
			int numOfRows,
			int incrementalLookbackDays,
			int backfillLookbackDays
	) {
		this.client = client;
		this.mapper = mapper;
		this.imageService = imageService;
		this.animalReportRepository = animalReportRepository;
		this.eventPublisher = eventPublisher;
		this.sourceType = sourceType;
		this.reportType = reportType;
		this.numOfRows = Math.max(numOfRows, 1);
		this.incrementalLookbackDays = Math.max(incrementalLookbackDays, 1);
		this.backfillLookbackDays = Math.max(backfillLookbackDays, 1);
	}

	public AnimalPublicApiSyncResult syncBackfill() {
		LocalDate endDate = LocalDate.now();
		return sync(endDate.minusDays(backfillLookbackDays), endDate);
	}

	public AnimalPublicApiSyncResult syncIncremental() {
		LocalDate endDate = LocalDate.now();
		return sync(endDate.minusDays(incrementalLookbackDays), endDate);
	}

	@Transactional
	public AnimalPublicApiSyncResult sync(LocalDate startDate, LocalDate endDate) {
		int pageNo = 1;
		int fetchedCount = 0;
		int savedCount = 0;
		int skippedCount = 0;
		int pageCount = 0;

		while (true) {
			AnimalPublicApiPage page = client.fetch(startDate, endDate, pageNo, numOfRows);
			pageCount++;
			if (page.records().isEmpty()) {
				break;
			}
			for (AnimalPublicApiRecord record : page.records()) {
				fetchedCount++;
				if (saveIfNew(record)) {
					savedCount++;
				} else {
					skippedCount++;
				}
			}
			pageNo++;
		}

		return new AnimalPublicApiSyncResult(fetchedCount, savedCount, skippedCount, pageCount);
	}

	private boolean saveIfNew(AnimalPublicApiRecord record) {
		if (record == null || !StringUtils.hasText(record.externalId())) {
			return false;
		}
		String externalId = record.externalId().trim();
		if (animalReportRepository.existsByApiProviderAndExternalId(sourceType, externalId)) {
			return false;
		}

		try {
			AnimalReport report = mapper.toReport(record, sourceType, reportType);
			AnimalReport saved = animalReportRepository.save(report);
			imageService.saveExternalImageIfPresent(saved, record.imageUrl());
			eventPublisher.publishEvent(new AnimalReportCreatedEvent(saved.getReportId()));
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("Duplicate animal public API record. sourceType={}, externalId={}", sourceType, externalId, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("Animal public API record could not be saved. sourceType={}, externalId={}, reason={}",
					sourceType, externalId, exception.getMessage());
			return false;
		}
	}
}
