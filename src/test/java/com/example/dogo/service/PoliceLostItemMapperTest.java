package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.dto.PoliceLostItemResponse;
import com.example.dogo.entity.LostItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliceLostItemMapperTest {

	private final PoliceLostItemMapper mapper = new PoliceLostItemMapper();

	@Test
	void mapsPoliceResponseToLostItem() {
		PoliceLostItemResponse response = new PoliceLostItemResponse(
				"L202605090000001",
				"검정 가방을 찾습니다",
				"노트북 가방",
				"2026-05-08",
				"강남역",
				"가방 > 기타가방"
		);

		LostItem lostItem = mapper.toLostItem(response);

		assertThat(lostItem.getSourceType()).isEqualTo("POLICE");
		assertThat(lostItem.getAtcId()).isEqualTo("L202605090000001");
		assertThat(lostItem.getUser()).isNull();
		assertThat(lostItem.getTitle()).isEqualTo("검정 가방을 찾습니다");
		assertThat(lostItem.getItemName()).isEqualTo("노트북 가방");
		assertThat(lostItem.getLostAt()).isEqualTo(LocalDateTime.of(2026, 5, 8, 0, 0));
		assertThat(lostItem.getLostPlace()).isEqualTo("강남역");
		assertThat(lostItem.getCategoryMain()).isEqualTo("가방");
		assertThat(lostItem.getCategorySub()).isEqualTo("기타가방");
		assertThat(lostItem.getStatus()).isEqualTo("WAITING");
		assertThat(lostItem.isDeleted()).isFalse();
	}

	@Test
	void mapsCompactDateAndTrimsCategoryNames() {
		PoliceLostItemResponse response = new PoliceLostItemResponse(
				" L202605100000002 ",
				"검정 지갑",
				"카드 지갑",
				"20260510",
				" ",
				" 지갑 > 카드지갑 "
		);

		LostItem lostItem = mapper.toLostItem(response);

		assertThat(lostItem.getAtcId()).isEqualTo("L202605100000002");
		assertThat(lostItem.getLostAt()).isEqualTo(LocalDateTime.of(2026, 5, 10, 0, 0));
		assertThat(lostItem.getLostPlace()).isEqualTo("장소 미상");
		assertThat(lostItem.getCategoryMain()).isEqualTo("지갑");
		assertThat(lostItem.getCategorySub()).isEqualTo("카드지갑");
	}

	@Test
	void mapsDetailResponseFieldsWhenPresent() {
		PoliceLostItemResponse listResponse = new PoliceLostItemResponse(
				"L2018120100000706",
				"목록 제목",
				"목록 물품명",
				"2018-12-01",
				"목록 장소",
				"가방 > 기타가방"
		);
		PoliceLostItemDetailResponse detailResponse = new PoliceLostItemDetailResponse(
				"L2018120100000706",
				"루이까또즈 남성용 반지갑(블루(파랑)색)을 분실하였습니다.",
				"루이까또즈 남성용 반지갑",
				"2018-12-01",
				"21",
				"대흥동 택시 안",
				"지갑 > 남성용 지갑",
				"담당자 접수",
				"개인정보보호정책에 의해 정보가 제공되지 않습니다.",
				"대전광역시",
				"O0000673",
				"대전역지구대",
				"042-271-0112",
				"택시",
				"https://minwon24.police.go.kr/images/sub/img02_no_img.gif"
		);

		LostItem lostItem = mapper.toLostItem(listResponse, detailResponse);

		assertThat(lostItem.getAtcId()).isEqualTo("L2018120100000706");
		assertThat(lostItem.getTitle()).isEqualTo("루이까또즈 남성용 반지갑(블루(파랑)색)을 분실하였습니다.");
		assertThat(lostItem.getItemName()).isEqualTo("루이까또즈 남성용 반지갑");
		assertThat(lostItem.getContent()).isEqualTo("개인정보보호정책에 의해 정보가 제공되지 않습니다.");
		assertThat(lostItem.getLostAt()).isEqualTo(LocalDateTime.of(2018, 12, 1, 21, 0));
		assertThat(lostItem.getLostArea()).isEqualTo("대전광역시");
		assertThat(lostItem.getLostPlace()).isEqualTo("대흥동 택시 안");
		assertThat(lostItem.getCategoryMain()).isEqualTo("지갑");
		assertThat(lostItem.getCategorySub()).isEqualTo("남성용 지갑");
		assertThat(lostItem.getContact()).isEqualTo("대전역지구대 / 042-271-0112");
	}

	@Test
	void rejectsResponseWithoutAtcId() {
		PoliceLostItemResponse response = new PoliceLostItemResponse(
				" ",
				"검정 가방",
				"노트북 가방",
				"2026-05-08",
				"강남역",
				"가방 > 기타가방"
		);

		assertThatThrownBy(() -> mapper.toLostItem(response))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("경찰청 분실물 접수 ID가 없습니다.");
	}
}
