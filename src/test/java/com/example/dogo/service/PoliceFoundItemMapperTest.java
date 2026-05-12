package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemResponse;
import com.example.dogo.entity.FoundItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliceFoundItemMapperTest {

	private final PoliceFoundItemMapper mapper = new PoliceFoundItemMapper();

	@Test
	void mapsPoliceResponseToFoundItem() {
		PoliceFoundItemResponse response = item("F202605110000001", "1");

		FoundItem foundItem = mapper.toFoundItem(response);

		assertThat(foundItem.getSourceType()).isEqualTo("POLICE");
		assertThat(foundItem.getAtcId()).isEqualTo("F202605110000001");
		assertThat(foundItem.getFdSn()).isEqualTo(1);
		assertThat(foundItem.getUser()).isNull();
		assertThat(foundItem.getTitle()).isEqualTo("빵 봉투(브라운(갈)색)을 습득하여 보관하고 있습니다");
		assertThat(foundItem.getItemName()).isEqualTo("빵 봉투");
		assertThat(foundItem.getColorName()).isEqualTo("브라운(갈)");
		assertThat(foundItem.getFoundAt()).isEqualTo(LocalDateTime.of(2026, 5, 11, 0, 0));
		assertThat(foundItem.getKeepPlace()).isEqualTo("서울역(한국철도공사)");
		assertThat(foundItem.getCategoryMain()).isEqualTo("쇼핑백");
		assertThat(foundItem.getCategorySub()).isEqualTo("쇼핑백");
		assertThat(foundItem.getStatus()).isEqualTo("KEEPING");
	}

	@Test
	void mapsDetailResponseFieldsWhenPresent() {
		FoundItem foundItem = mapper.toFoundItem(item("F202605110000001", "1"), detail("보관중"));

		assertThat(foundItem.getAtcId()).isEqualTo("F202605110000001");
		assertThat(foundItem.getFdSn()).isEqualTo(1);
		assertThat(foundItem.getContent()).isEqualTo("특이사항 : 없음");
		assertThat(foundItem.getFoundAt()).isEqualTo(LocalDateTime.of(2026, 5, 11, 20, 0));
		assertThat(foundItem.getFoundPlace()).isEqualTo("기차");
		assertThat(foundItem.getKeepPlace()).isEqualTo("서울역(한국철도공사)");
		assertThat(foundItem.getContact()).isEqualTo("서울역(한국철도공사) / 02-3149-2531");
		assertThat(foundItem.getCustodyStatus()).isEqualTo("보관중");
		assertThat(foundItem.getReceiveType()).isEqualTo("기관보관");
		assertThat(foundItem.getStatus()).isEqualTo("KEEPING");
	}

	@Test
	void mapsReturnedCustodyStatusToReturned() {
		FoundItem foundItem = mapper.toFoundItem(item("F202605110000001", "1"), detail("수령완료"));

		assertThat(foundItem.getStatus()).isEqualTo("RETURNED");
	}

	@Test
	void rejectsResponseWithoutFoundSequence() {
		PoliceFoundItemResponse response = item("F202605110000001", " ");

		assertThatThrownBy(() -> mapper.toFoundItem(response))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("경찰청 습득물 습득순번이 없습니다.");
	}

	private PoliceFoundItemResponse item(String atcId, String fdSn) {
		return new PoliceFoundItemResponse(
				atcId,
				"브라운(갈)",
				"서울역(한국철도공사)",
				"https://example.com/found/bread.jpg",
				"빵 봉투",
				"빵 봉투(브라운(갈)색)을 습득하여 보관하고 있습니다",
				fdSn,
				"2026-05-11",
				"쇼핑백 > 쇼핑백"
		);
	}

	private PoliceFoundItemDetailResponse detail(String custodyStatus) {
		return new PoliceFoundItemDetailResponse(
				"F202605110000001",
				custodyStatus,
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
