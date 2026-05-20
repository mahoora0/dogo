package com.example.dogo.service.missing.sync;

import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.service.missing.client.Safe182MissingPersonClient;
import com.example.dogo.service.missing.client.Safe182MissingPersonRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class Safe182MissingPersonSyncService {

	public static final String API_PROVIDER = "SAFE182_FIND_CHILD";

	private static final int SEARCH_PAGE = 1;
	private static final int SEARCH_ROW_SIZE = 20;
	private static final String UNKNOWN = "미상";

	private final Safe182MissingPersonClient client;
	private final MissingPersonRepository repository;

	public Safe182MissingPersonSyncService(Safe182MissingPersonClient client, MissingPersonRepository repository) {
		this.client = client;
		this.repository = repository;
	}

	@Transactional
	public Safe182MissingPersonSyncResult syncSearch(String keyword) {
		var page = client.search(keyword, SEARCH_PAGE, SEARCH_ROW_SIZE);
		int imported = 0;
		int skipped = 0;
		for (Safe182MissingPersonRecord record : page.records()) {
			if (!StringUtils.hasText(record.externalId())) {
				skipped++;
				continue;
			}
			if (repository.findByApiProviderAndExternalId(API_PROVIDER, record.externalId()).isPresent()) {
				skipped++;
				continue;
			}

			repository.save(toReport(record));
			imported++;
		}
		return new Safe182MissingPersonSyncResult(imported, skipped);
	}

	private MissingPersonReport toReport(Safe182MissingPersonRecord record) {
		return MissingPersonReport.fromPublicApi(
				API_PROVIDER,
				record.externalId(),
				record.rawPayload(),
				record.name(),
				record.gender(),
				record.age() != null ? record.age() : 0,
				UNKNOWN,
				record.occurredAt(),
				textOrUnknown(record.occurredPlace()),
				record.heightCm(),
				record.weightKg(),
				textOrUnknown(record.bodyType()),
				textOrUnknown(record.faceShape()),
				textOrUnknown(record.hairColor()),
				textOrUnknown(record.hairStyle()),
				textOrUnknown(record.clothing())
		);
	}

	private String textOrUnknown(String value) {
		return StringUtils.hasText(value) ? value.trim() : UNKNOWN;
	}
}
