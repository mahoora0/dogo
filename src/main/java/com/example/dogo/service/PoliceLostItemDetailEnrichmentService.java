package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.dto.PoliceLostItemResponse;
import com.example.dogo.entity.LostItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class PoliceLostItemDetailEnrichmentService {

	private static final Logger log = LoggerFactory.getLogger(PoliceLostItemDetailEnrichmentService.class);
	private static final String POLICE_SOURCE_TYPE = "POLICE";

	private final PoliceLostItemClient client;
	private final PoliceLostItemMapper mapper;
	private final PoliceLostItemImageService imageService;

	public PoliceLostItemDetailEnrichmentService(
			PoliceLostItemClient client,
			PoliceLostItemMapper mapper,
			PoliceLostItemImageService imageService
	) {
		this.client = client;
		this.mapper = mapper;
		this.imageService = imageService;
	}

	public void enrichIfNeeded(LostItem lostItem) {
		if (!needsEnrichment(lostItem)) {
			return;
		}

		try {
			Optional<PoliceLostItemDetailResponse> detail = client.fetchLostItemDetail(lostItem.getAtcId());
			if (detail.isEmpty()) {
				return;
			}

			LostItem enriched = mapper.toLostItem(toListResponse(lostItem), detail.get());
			lostItem.updatePoliceDetail(
					enriched.getTitle(),
					enriched.getContent(),
					enriched.getItemName(),
					enriched.getCategoryMain(),
					enriched.getCategorySub(),
					enriched.getColorName(),
					enriched.getLostAt(),
					enriched.getLostArea(),
					enriched.getLostPlace(),
					enriched.getContact()
			);
			imageService.saveImageIfPresent(lostItem, detail.get());
		} catch (RuntimeException exception) {
			log.warn("경찰청 분실물 상세 보강에 실패했습니다. atcId={}", lostItem.getAtcId(), exception);
		}
	}

	private boolean needsEnrichment(LostItem lostItem) {
		return lostItem != null
				&& POLICE_SOURCE_TYPE.equals(lostItem.getSourceType())
				&& StringUtils.hasText(lostItem.getAtcId())
				&& (!StringUtils.hasText(lostItem.getContent())
				|| !StringUtils.hasText(lostItem.getColorName())
				|| !StringUtils.hasText(lostItem.getLostArea())
				|| !StringUtils.hasText(lostItem.getContact()));
	}

	private PoliceLostItemResponse toListResponse(LostItem lostItem) {
		return new PoliceLostItemResponse(
				lostItem.getAtcId(),
				lostItem.getTitle(),
				lostItem.getItemName(),
				lostItem.getLostAt().toLocalDate().toString(),
				lostItem.getLostPlace(),
				categoryName(lostItem)
		);
	}

	private String categoryName(LostItem lostItem) {
		if (StringUtils.hasText(lostItem.getCategoryMain()) && StringUtils.hasText(lostItem.getCategorySub())) {
			return lostItem.getCategoryMain() + " > " + lostItem.getCategorySub();
		}
		return lostItem.getCategoryMain();
	}
}
