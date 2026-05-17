package com.example.dogo.dto.police;

public record PoliceLostItemResponse(
		String atcId,
		String lstSbjt,
		String lstPrdtNm,
		String lstYmd,
		String lstPlace,
		String prdtClNm
) {
}
