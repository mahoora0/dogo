package com.example.dogo.config;

import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.repository.missing.MissingPersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class MissingPersonSearchBackfiller implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MissingPersonSearchBackfiller.class);

	private final MissingPersonRepository missingPersonRepository;

	public MissingPersonSearchBackfiller(MissingPersonRepository missingPersonRepository) {
		this.missingPersonRepository = missingPersonRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		backfill();
	}

	public void backfill() {
		log.info("Checking for missing SEARCH_CONTENT and corrupted data in MISSING_PERSON_REPORT...");
		try {
			List<MissingPersonReport> reports = missingPersonRepository.findAll();
			long searchBackfillCount = 0;
			long dataCleanupCount = 0;
			for (MissingPersonReport report : reports) {
				boolean searchModified = false;
				boolean dataModified = report.cleanUpData();

				if (report.getSearchContent() == null || report.getSearchContent().isBlank()) {
					report.generateSearchContent();
					searchModified = true;
				}

				if (searchModified || dataModified) {
					missingPersonRepository.save(report);
					if (searchModified) {
						searchBackfillCount++;
					}
					if (dataModified) {
						dataCleanupCount++;
					}
				}
			}

			if (searchBackfillCount > 0) {
				log.info("Successfully backfilled SEARCH_CONTENT for {} missing person reports.", searchBackfillCount);
			} else {
				log.info("No SEARCH_CONTENT backfill needed.");
			}

			if (dataCleanupCount > 0) {
				log.info("Successfully cleaned up corrupted data for {} missing person reports.", dataCleanupCount);
			} else {
				log.info("No corrupted data cleanup needed.");
			}
		} catch (Exception exception) {
			log.error("Failed to backfill or clean up missing person reports.", exception);
		}
	}
}
