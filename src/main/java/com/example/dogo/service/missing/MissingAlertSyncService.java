package com.example.dogo.service.missing;

import com.example.dogo.service.missing.client.Safe182MissingPersonClient;
import com.example.dogo.service.missing.client.Safe182MissingPersonPage;
import com.example.dogo.service.missing.client.Safe182MissingPersonRecord;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MissingAlertSyncService {

	private static final Logger log = LoggerFactory.getLogger(MissingAlertSyncService.class);
	private static final String API_PROVIDER = "SAFE182_MISSING_PERSON";

	private final Safe182MissingPersonClient missingPersonClient;
	private final MissingPersonRepository missingPersonRepository;
	private final int rowSize;
	private final int backfillLookbackDays;
	private final int incrementalLookbackDays;

	public MissingAlertSyncService(
			Safe182MissingPersonClient missingPersonClient,
			MissingPersonRepository missingPersonRepository,
			@Value("${safe182.missing-alert.num-of-rows:100}") int rowSize,
			@Value("${safe182.missing-alert.backfill-lookback-days:1}") int backfillLookbackDays,
			@Value("${safe182.missing-alert.incremental-lookback-days:7}") int incrementalLookbackDays
	) {
		this.missingPersonClient = missingPersonClient;
		this.missingPersonRepository = missingPersonRepository;
		this.rowSize = Math.max(1, Math.min(rowSize, 100));
		this.backfillLookbackDays = Math.max(backfillLookbackDays, 1);
		this.incrementalLookbackDays = Math.max(incrementalLookbackDays, 1);
	}

	public MissingAlertSyncResult syncBackfillFromToday() {
		return new MissingAlertSyncResult(0, 0, 0, 0);
	}

	MissingAlertSyncResult syncAll() {
		return new MissingAlertSyncResult(0, 0, 0, 0);
	}

	public MissingAlertSyncResult syncIncrementalFromToday() {
		return new MissingAlertSyncResult(0, 0, 0, 0);
	}

	MissingAlertSyncResult syncBackfill(LocalDate endDate) {
		return new MissingAlertSyncResult(0, 0, 0, 0);
	}

	MissingAlertSyncResult syncDate(LocalDate date) {
		return new MissingAlertSyncResult(0, 0, 0, 0);
	}

	private MissingAlertSyncResult syncRange(LocalDate startDate, LocalDate endDate) {
		return new MissingAlertSyncResult(0, 0, 0, 0);
	}

	private boolean saveIfNew(Safe182MissingPersonRecord record) {
		if (record == null) {
			return false;
		}

		String externalId = "safe182-" + record.externalId();
		if (missingPersonRepository.existsByApiProviderAndExternalId(API_PROVIDER, externalId)) {
			return false;
		}

		try {
			MissingPersonReport report = MissingPersonReport.fromPublicApi(
					API_PROVIDER,
					externalId,
					record.rawPayload(),
					record.name(),
					record.gender(),
					record.age() != null ? record.age() : 0,
					"대한민국",
					record.occurredAt(),
					record.occurredPlace() != null ? record.occurredPlace() : "장소 미상",
					record.heightCm(),
					record.weightKg(),
					record.bodyType() != null ? record.bodyType() : "미상",
					record.faceShape() != null ? record.faceShape() : "미상",
					record.hairColor() != null ? record.hairColor() : "미상",
					record.hairStyle() != null ? record.hairStyle() : "미상",
					record.clothing() != null ? record.clothing() : "착의 정보 없음"
			);
			missingPersonRepository.save(report);
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("이미 저장된 실종아동 데이터입니다. externalId={}", externalId, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("실종아동 데이터를 저장하지 못했습니다. externalId={}, reason={}", externalId, exception.getMessage());
			return false;
		}
	}
}
