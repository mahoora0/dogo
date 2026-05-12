package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemResponse;
import com.example.dogo.entity.FoundItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class PoliceFoundItemMapper {

	public FoundItem toFoundItem(PoliceFoundItemResponse response) {
		if (response == null) {
			throw new IllegalArgumentException("경찰청 습득물 응답이 비어 있습니다.");
		}
		if (!StringUtils.hasText(response.atcId())) {
			throw new IllegalArgumentException("경찰청 습득물 관리 ID가 없습니다.");
		}

		Integer fdSn = parseRequiredInt(response.fdSn(), "경찰청 습득물 습득순번이 없습니다.");
		String itemName = defaultText(response.fdPrdtNm(), response.fdSbjt());
		if (!StringUtils.hasText(itemName)) {
			throw new IllegalArgumentException("경찰청 습득물 물품명이 없습니다.");
		}

		CategoryNames categoryNames = parseCategory(response.prdtClNm());
		return FoundItem.fromPolice(
				response.atcId().trim(),
				fdSn,
				defaultText(response.fdSbjt(), itemName),
				null,
				itemName,
				categoryNames.main(),
				categoryNames.sub(),
				blankToNull(response.clrNm()),
				parseFoundAt(response.fdYmd(), null),
				null,
				null,
				blankToNull(response.depPlace()),
				null,
				null,
				null,
				"KEEPING"
		);
	}

	public FoundItem toFoundItem(PoliceFoundItemResponse listResponse, PoliceFoundItemDetailResponse detailResponse) {
		if (detailResponse == null) {
			return toFoundItem(listResponse);
		}

		String atcId = defaultText(detailResponse.atcId(), listResponse == null ? null : listResponse.atcId());
		if (!StringUtils.hasText(atcId)) {
			throw new IllegalArgumentException("경찰청 습득물 관리 ID가 없습니다.");
		}

		Integer fdSn = parseRequiredInt(
				defaultText(detailResponse.fdSn(), listResponse == null ? null : listResponse.fdSn()),
				"경찰청 습득물 습득순번이 없습니다."
		);
		String itemName = defaultText(
				detailResponse.fdPrdtNm(),
				listResponse == null ? null : listResponse.fdPrdtNm()
		);
		itemName = defaultText(itemName, listResponse == null ? null : listResponse.fdSbjt());
		if (!StringUtils.hasText(itemName)) {
			throw new IllegalArgumentException("경찰청 습득물 물품명이 없습니다.");
		}

		String category = defaultText(
				detailResponse.prdtClNm(),
				listResponse == null ? null : listResponse.prdtClNm()
		);
		CategoryNames categoryNames = parseCategory(category);
		String title = defaultText(listResponse == null ? null : listResponse.fdSbjt(), itemName);
		String colorName = defaultText(detailResponse.clrNm(), listResponse == null ? null : listResponse.clrNm());

		return FoundItem.fromPolice(
				atcId.trim(),
				fdSn,
				title,
				blankToNull(detailResponse.uniq()),
				itemName,
				categoryNames.main(),
				categoryNames.sub(),
				colorName,
				parseFoundAt(defaultText(detailResponse.fdYmd(), listResponse == null ? null : listResponse.fdYmd()), detailResponse.fdHor()),
				null,
				blankToNull(detailResponse.fdPlace()),
				defaultText(detailResponse.depPlace(), listResponse == null ? null : listResponse.depPlace()),
				contact(detailResponse.orgNm(), detailResponse.tel()),
				blankToNull(detailResponse.csteSteNm()),
				blankToNull(detailResponse.fndKeepOrgnSeNm()),
				status(detailResponse.csteSteNm())
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

	Integer parseOptionalFdSn(String fdSn) {
		if (!StringUtils.hasText(fdSn)) {
			return null;
		}
		return parseRequiredInt(fdSn, "경찰청 습득물 습득순번이 올바르지 않습니다.");
	}

	private LocalDateTime parseFoundAt(String fdYmd, String fdHor) {
		if (!StringUtils.hasText(fdYmd)) {
			throw new IllegalArgumentException("경찰청 습득물 습득일자가 없습니다.");
		}

		String value = fdYmd.trim();
		try {
			LocalDate foundDate;
			if (value.matches("\\d{8}")) {
				foundDate = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
			} else {
				foundDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
			}
			return LocalDateTime.of(foundDate, parseFoundTime(fdHor));
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("경찰청 습득물 습득일자 형식이 올바르지 않습니다: " + fdYmd, exception);
		}
	}

	private LocalTime parseFoundTime(String fdHor) {
		if (!StringUtils.hasText(fdHor)) {
			return LocalTime.MIDNIGHT;
		}

		String value = fdHor.trim();
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

	private Integer parseRequiredInt(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(message, exception);
		}
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

	private String status(String custodyStatus) {
		String normalized = blankToNull(custodyStatus);
		if (normalized != null && (normalized.contains("수령") || normalized.contains("반환") || normalized.contains("종결"))) {
			return "RETURNED";
		}
		return "KEEPING";
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
