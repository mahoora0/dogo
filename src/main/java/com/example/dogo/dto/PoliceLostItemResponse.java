package com.example.dogo.dto;

public record PoliceLostItemResponse(
		String atcId,
		String lstSbjt,
		String lstPrdtNm,
		String lstYmd,
		String lstPlace,
		String prdtClNm
) {
}
