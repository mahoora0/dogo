package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.police.PoliceFoundItemResponse;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.service.police.client.PoliceFoundItemClient;
import com.example.dogo.service.police.mapper.PoliceFoundItemMapper;
import com.example.dogo.service.police.station.PoliceStationAddressResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoliceFoundItemDetailEnrichmentServiceTest {

	private PoliceFoundItemClient client;
	private PoliceFoundItemImageService imageService;
	private PoliceFoundItemDetailEnrichmentService enrichmentService;

	@BeforeEach
	void setUp() {
		client = mock(PoliceFoundItemClient.class);
		imageService = mock(PoliceFoundItemImageService.class);
		enrichmentService = new PoliceFoundItemDetailEnrichmentService(
				client,
				new PoliceFoundItemMapper(),
				new PoliceStationAddressResolver(),
				imageService
		);
	}

	@Test
	void enrichesPoliceFoundItemWhenDetailFieldsAreMissing() {
		FoundItem foundItem = listOnlyPoliceFoundItem();
		PoliceFoundItemDetailResponse detail = detail();
		when(client.fetchFoundItemDetail("F202605110000001", 1)).thenReturn(Optional.of(detail));

		enrichmentService.enrichIfNeeded(foundItem);

		assertThat(foundItem.getContent()).isEqualTo("특이사항 : 없음");
		assertThat(foundItem.getFoundAt()).isEqualTo(LocalDateTime.of(2026, 5, 11, 20, 0));
		assertThat(foundItem.getFoundPlace()).isEqualTo("기차");
		assertThat(foundItem.getContact()).isEqualTo("서울역(한국철도공사) / 02-3149-2531");
		assertThat(foundItem.getCustodyStatus()).isEqualTo("보관중");
		assertThat(foundItem.getReceiveType()).isEqualTo("기관보관");
		verify(imageService).saveImageIfPresent(foundItem, detail);
	}

	@Test
	void skipsWhenPoliceFoundItemAlreadyHasDetailFields() {
		FoundItem foundItem = listOnlyPoliceFoundItem();
		foundItem.updatePoliceDetail(
				foundItem.getTitle(),
				"상세 내용",
				foundItem.getItemName(),
				foundItem.getCategoryMain(),
				foundItem.getCategorySub(),
				foundItem.getColorName(),
				foundItem.getFoundAt(),
				foundItem.getFoundArea(),
				"기차",
				foundItem.getKeepPlace(),
				"서울역(한국철도공사) / 02-3149-2531",
				"보관중",
				"기관보관",
				foundItem.getStatus()
		);

		enrichmentService.enrichIfNeeded(foundItem);

		verify(client, never()).fetchFoundItemDetail("F202605110000001", 1);
		verify(imageService, never()).saveImageIfPresent(foundItem, detail());
	}

	private FoundItem listOnlyPoliceFoundItem() {
		return new PoliceFoundItemMapper().toFoundItem(new PoliceFoundItemResponse(
				"F202605110000001",
				"브라운(갈)",
				"서울역(한국철도공사)",
				"https://example.com/found/bread.jpg",
				"빵 봉투",
				"빵 봉투(브라운(갈)색)을 습득하여 보관하고 있습니다",
				"1",
				"2026-05-11",
				"쇼핑백 > 쇼핑백"
		));
	}

	private PoliceFoundItemDetailResponse detail() {
		return new PoliceFoundItemDetailResponse(
				"F202605110000001",
				"보관중",
				"서울역(한국철도공사)",
				"https://example.com/found/bread.jpg",
				"20",
				"기차",
				"빵 봉투",
				"1",
				"2026-05-11",
				"기관보관",
				"O0000001",
				"서울역(한국철도공사)",
				"쇼핑백 > 쇼핑백",
				"02-3149-2531",
				"특이사항 : 없음",
				"브라운(갈)"
		);
	}
}
