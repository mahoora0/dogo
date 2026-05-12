package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemResponse;
import com.example.dogo.entity.FoundItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class PoliceFoundItemDetailEnrichmentService {

	private static final Logger log = LoggerFactory.getLogger(PoliceFoundItemDetailEnrichmentService.class);
	private static final String POLICE_SOURCE_TYPE = "POLICE";

	private final PoliceFoundItemClient client;
	private final PoliceFoundItemMapper mapper;
	private final PoliceFoundItemImageService imageService;

	public PoliceFoundItemDetailEnrichmentService(
			PoliceFoundItemClient client,
			PoliceFoundItemMapper mapper,
			PoliceFoundItemImageService imageService
	) {
		this.client = client;
		this.mapper = mapper;
		this.imageService = imageService;
	}

	public void enrichIfNeeded(FoundItem foundItem) {
		if (!needsEnrichment(foundItem)) {
			return;
		}

		try {
			Optional<PoliceFoundItemDetailResponse> detail = client.fetchFoundItemDetail(foundItem.getAtcId(), foundItem.getFdSn());
			if (detail.isEmpty()) {
				return;
			}

			FoundItem enriched = mapper.toFoundItem(toListResponse(foundItem), detail.get());
			foundItem.updatePoliceDetail(
					enriched.getTitle(),
					enriched.getContent(),
					enriched.getItemName(),
					enriched.getCategoryMain(),
					enriched.getCategorySub(),
					enriched.getColorName(),
					enriched.getFoundAt(),
					enriched.getFoundArea(),
					enriched.getFoundPlace(),
					enriched.getKeepPlace(),
					enriched.getContact(),
					enriched.getCustodyStatus(),
					enriched.getReceiveType(),
					enriched.getStatus()
			);
			imageService.saveImageIfPresent(foundItem, detail.get());
		} catch (RuntimeException exception) {
			log.warn("경찰청 습득물 상세 보강에 실패했습니다. atcId={}, fdSn={}", foundItem.getAtcId(), foundItem.getFdSn(), exception);
		}
	}

	private boolean needsEnrichment(FoundItem foundItem) {
		return foundItem != null
				&& POLICE_SOURCE_TYPE.equals(foundItem.getSourceType())
				&& StringUtils.hasText(foundItem.getAtcId())
				&& foundItem.getFdSn() != null
				&& (!StringUtils.hasText(foundItem.getContent())
				|| !StringUtils.hasText(foundItem.getFoundPlace())
				|| !StringUtils.hasText(foundItem.getContact()));
	}

	private PoliceFoundItemResponse toListResponse(FoundItem foundItem) {
		return new PoliceFoundItemResponse(
				foundItem.getAtcId(),
				foundItem.getColorName(),
				foundItem.getKeepPlace(),
				null,
				foundItem.getItemName(),
				foundItem.getTitle(),
				String.valueOf(foundItem.getFdSn()),
				foundItem.getFoundAt().toLocalDate().toString(),
				categoryName(foundItem)
		);
	}

	private String categoryName(FoundItem foundItem) {
		if (StringUtils.hasText(foundItem.getCategoryMain()) && StringUtils.hasText(foundItem.getCategorySub())) {
			return foundItem.getCategoryMain() + " > " + foundItem.getCategorySub();
		}
		return foundItem.getCategoryMain();
	}
}
