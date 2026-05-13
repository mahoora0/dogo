package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.dto.PoliceLostItemResponse;
import com.example.dogo.entity.LostItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoliceLostItemDetailEnrichmentServiceTest {

	private PoliceLostItemClient client;
	private PoliceLostItemImageService imageService;
	private PoliceLostItemDetailEnrichmentService enrichmentService;

	@BeforeEach
	void setUp() {
		client = mock(PoliceLostItemClient.class);
		imageService = mock(PoliceLostItemImageService.class);
		enrichmentService = new PoliceLostItemDetailEnrichmentService(
				client,
				new PoliceLostItemMapper(),
				imageService
		);
	}

	@Test
	void enrichesPoliceLostItemWhenDetailFieldsAreMissing() {
		LostItem lostItem = listOnlyPoliceLostItem();
		PoliceLostItemDetailResponse detail = detail();
		when(client.fetchLostItemDetail("L2018120100000706")).thenReturn(Optional.of(detail));

		enrichmentService.enrichIfNeeded(lostItem);

		assertThat(lostItem.getTitle()).isEqualTo("루이까또즈 남성용 반지갑(블루(파랑)색)을 분실하였습니다.");
		assertThat(lostItem.getItemName()).isEqualTo("루이까또즈 남성용 반지갑");
		assertThat(lostItem.getContent()).isEqualTo("개인정보보호정책에 의해 정보가 제공되지 않습니다.");
		assertThat(lostItem.getLostAt()).isEqualTo(LocalDateTime.of(2018, 12, 1, 21, 0));
		assertThat(lostItem.getLostArea()).isEqualTo("대전광역시");
		assertThat(lostItem.getLostPlace()).isEqualTo("대흥동 택시 안");
		assertThat(lostItem.getCategoryMain()).isEqualTo("지갑");
		assertThat(lostItem.getCategorySub()).isEqualTo("남성용 지갑");
		assertThat(lostItem.getColorName()).isEqualTo("블루(파랑)");
		assertThat(lostItem.getContact()).isEqualTo("대전역지구대 / 042-271-0112");
		verify(imageService).saveImageIfPresent(lostItem, detail);
	}

	@Test
	void skipsWhenPoliceLostItemAlreadyHasDetailFields() {
		LostItem lostItem = listOnlyPoliceLostItem();
		lostItem.updatePoliceDetail(
				lostItem.getTitle(),
				"상세 내용",
				lostItem.getItemName(),
				lostItem.getCategoryMain(),
				lostItem.getCategorySub(),
				"블루(파랑)",
				lostItem.getLostAt(),
				"서울특별시",
				lostItem.getLostPlace(),
				"서울역지구대 / 02-0000-0000"
		);

		enrichmentService.enrichIfNeeded(lostItem);

		verify(client, never()).fetchLostItemDetail("L2018120100000706");
		verify(imageService, never()).saveImageIfPresent(lostItem, detail());
	}

	private LostItem listOnlyPoliceLostItem() {
		return new PoliceLostItemMapper().toLostItem(new PoliceLostItemResponse(
				"L2018120100000706",
				"목록 제목",
				"목록 물품명",
				"2018-12-01",
				"목록 장소",
				"가방 > 기타가방"
		));
	}

	private PoliceLostItemDetailResponse detail() {
		return new PoliceLostItemDetailResponse(
				"L2018120100000706",
				"루이까또즈 남성용 반지갑(블루(파랑)색)을 분실하였습니다.",
				"루이까또즈 남성용 반지갑",
				"2018-12-01",
				"21",
				"대흥동 택시 안",
				"지갑 > 남성용 지갑",
				"담당자 접수",
				"블루(파랑)",
				"개인정보보호정책에 의해 정보가 제공되지 않습니다.",
				"대전광역시",
				"O0000673",
				"대전역지구대",
				"042-271-0112",
				"택시",
				"https://example.com/lost/wallet.jpg"
		);
	}
}
