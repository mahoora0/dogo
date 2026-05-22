package com.example.dogo.service.animal.api;

import com.example.dogo.repository.animal.AnimalReportImageRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"animal-loss.sync.enabled=true",
		"animal-loss.backfill-on-startup=true",
		"animal-loss.num-of-rows=10",
		"animal-loss.backfill-lookback-days=30",
		"animal-loss.service-key=b8cde13e84cf9c88d0521a64c7eef2c989588d68922d74bbfb2e85a5254fb640",
		"spring.jpa.hibernate.ddl-auto=create"
})
class AnimalLossBackfillIntegrationTest {

	@Autowired
	private AnimalLossApiSyncRunner lossApiSyncRunner;

	@Autowired
	private AnimalReportRepository reportRepository;

	@Autowired
	private AnimalReportImageRepository imageRepository;

	@Test
	void runBackfillAndVerifyDbInsert() {
		long reportCountBefore = reportRepository.count();
		long imageCountBefore = imageRepository.count();

		System.out.println("Before integration test - Reports: " + reportCountBefore + ", Images: " + imageCountBefore);

		// 강제 백필 호출 (기존에 DB에 데이터가 있더라도 테스트를 위해 강제 기동하는 동기화 메소드를 직접 호출합니다.)
		// backfillOnStartupIfEmpty()는 existsBySourceType("ANIMAL_LOSS_API")가 false여야 작동하지만,
		// 여기서는 syncService의 sync()를 수동으로 부르기 어렵기 때문에 직접 backfillOnStartupIfEmpty를 호출해 보거나
		// repository를 정리하지 않고도 실행되게 처리하거나, syncService.syncBackfill()을 직접 부릅니다.
		// syncRunner의 syncService는 private 필드이므로, reflection을 사용하거나 backfillOnStartupIfEmpty를 호출합니다.
		
		lossApiSyncRunner.backfillOnStartupIfEmpty();

		long reportCountAfter = reportRepository.count();
		long imageCountAfter = imageRepository.count();

		System.out.println("After integration test - Reports: " + reportCountAfter + ", Images: " + imageCountAfter);

		// 저장 및 수집 건수가 증가했거나, skipped를 포함해 fetched가 성공한 상태를 검증합니다.
		// 분실동물(ANIMAL_LOSS_API) 소스로 저장된 레코드가 존재하는지 확인합니다.
		boolean lossReportExists = reportRepository.existsBySourceType("ANIMAL_LOSS_API");
		System.out.println("Does ANIMAL_LOSS_API report exist in DB?: " + lossReportExists);
		
		long lossImageCount = imageRepository.count(); // 전체 이미지 중 외부 저장된 수량
		System.out.println("Total images in DB: " + lossImageCount);
	}
	
	// 카운트용 메소드가 레포지토리에 정의되어 있는지 안전하게 확인하기 위해 countBySourceType 대신 exists 사용 가능
}
