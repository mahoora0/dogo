package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemResponse;
import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.entity.LostItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class PoliceLostItemMapper {

	private static final String UNKNOWN_PLACE = "장소 미상";

	public LostItem toLostItem(PoliceLostItemResponse response) {
		if (response == null) {
			throw new IllegalArgumentException("경찰청 분실물 응답이 비어 있습니다.");
		}
		if (!StringUtils.hasText(response.atcId())) {
			throw new IllegalArgumentException("경찰청 분실물 접수 ID가 없습니다.");
		}

		CategoryNames categoryNames = parseCategory(response.prdtClNm());
		String itemName = defaultText(response.lstPrdtNm(), response.lstSbjt());

		if (!StringUtils.hasText(itemName)) {
			throw new IllegalArgumentException("경찰청 분실물 물품명이 없습니다.");
		}

		return LostItem.fromPolice(
				response.atcId().trim(),
				defaultText(response.lstSbjt(), itemName),
				null,
				itemName,
				categoryNames.main(),
				categoryNames.sub(),
				parseLostAt(response.lstYmd()),
				null,
				defaultText(response.lstPlace(), UNKNOWN_PLACE),
				null
		);
	}

	public LostItem toLostItem(PoliceLostItemResponse listResponse, PoliceLostItemDetailResponse detailResponse) {
		if (detailResponse == null) {
			return toLostItem(listResponse);
		}

		String atcId = defaultText(detailResponse.atcId(), listResponse == null ? null : listResponse.atcId());
		if (!StringUtils.hasText(atcId)) {
			throw new IllegalArgumentException("경찰청 분실물 접수 ID가 없습니다.");
		}

		String itemName = defaultText(
				detailResponse.lstPrdtNm(),
				listResponse == null ? null : listResponse.lstPrdtNm()
		);
		itemName = defaultText(itemName, detailResponse.lstSbjt());
		if (!StringUtils.hasText(itemName)) {
			throw new IllegalArgumentException("경찰청 분실물 물품명이 없습니다.");
		}

		String category = defaultText(
				detailResponse.prdtClNm(),
				listResponse == null ? null : listResponse.prdtClNm()
		);
		CategoryNames categoryNames = parseCategory(category);

		return LostItem.fromPolice(
				atcId,
				defaultText(detailResponse.lstSbjt(), listResponse == null ? itemName : defaultText(listResponse.lstSbjt(), itemName)),
				blankToNull(detailResponse.uniq()),
				itemName,
				categoryNames.main(),
				categoryNames.sub(),
				parseLostAt(defaultText(detailResponse.lstYmd(), listResponse == null ? null : listResponse.lstYmd()), detailResponse.lstHor()),
				blankToNull(detailResponse.lstLctNm()),
				defaultText(detailResponse.lstPlace(), listResponse == null ? UNKNOWN_PLACE : defaultText(listResponse.lstPlace(), UNKNOWN_PLACE)),
				contact(detailResponse.orgNm(), detailResponse.tel())
		);
	}

	CategoryNames parseCategory(String prdtClNm) {
		if (!StringUtils.hasText(prdtClNm)) {
			return new CategoryNames(null, null);
		}

		String[] parts = prdtClNm.split(">");
		String main = blankToNull(parts[0]);
		String sub = parts.length > 1 ? blankToNull(parts[1]) : null;
		return new CategoryNames(main, sub);
	}

	private LocalDateTime parseLostAt(String lstYmd) {
		return parseLostAt(lstYmd, null);
	}

	private LocalDateTime parseLostAt(String lstYmd, String lstHor) {
		if (!StringUtils.hasText(lstYmd)) {
			throw new IllegalArgumentException("경찰청 분실물 분실일이 없습니다.");
		}

		String value = lstYmd.trim();
		try {
			LocalDate lostDate;
			if (value.matches("\\d{8}")) {
				lostDate = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
			} else {
				lostDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
			}
			return LocalDateTime.of(lostDate, parseLostTime(lstHor));
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("경찰청 분실물 분실일 형식이 올바르지 않습니다: " + lstYmd, exception);
		}
	}

	private LocalTime parseLostTime(String lstHor) {
		if (!StringUtils.hasText(lstHor)) {
			return LocalTime.MIDNIGHT;
		}

		String value = lstHor.trim();
		try {
			if (value.matches("\\d{1,2}")) {
				int hour = Integer.parseInt(value);
				if (hour >= 0 && hour <= 23) {
					return LocalTime.of(hour, 0);
				}
			}
			if (value.matches("\\d{1,2}:\\d{2}")) {
				return LocalTime.parse(value);
			}
		} catch (RuntimeException ignored) {
			return LocalTime.MIDNIGHT;
		}
		return LocalTime.MIDNIGHT;
	}

	private String contact(String orgNm, String tel) {
		String normalizedOrg = blankToNull(orgNm);
		String normalizedTel = blankToNull(tel);
		if (normalizedOrg != null && normalizedTel != null) {
			return normalizedOrg + " / " + normalizedTel;
		}
		if (normalizedTel != null) {
			return normalizedTel;
		}
		return normalizedOrg;
	}

	private String defaultText(String value, String fallback) {
		String normalized = blankToNull(value);
		if (normalized != null) {
			return normalized;
		}
		return blankToNull(fallback);
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	record CategoryNames(String main, String sub) {
	}
}
