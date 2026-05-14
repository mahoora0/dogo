package com.example.dogo.dto.police;

public record PoliceFoundItemResponse(
		String atcId,
		String clrNm,
		String depPlace,
		String fdFilePathImg,
		String fdPrdtNm,
		String fdSbjt,
		String fdSn,
		String fdYmd,
		String prdtClNm
) {
}
