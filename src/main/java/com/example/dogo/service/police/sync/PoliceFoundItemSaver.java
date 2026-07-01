package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.police.PoliceFoundItemResponse;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.service.match.FoundItemMatchRequestedEvent;
import com.example.dogo.service.police.mapper.PoliceFoundItemMapper;
import com.example.dogo.service.police.station.PoliceStationAddressResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 경찰청 습득물 단건 저장 컴포넌트.
 * saveIfNew를 별도 빈으로 분리하여 Spring AOP 프록시를 통해
 * 트랜잭션이 올바르게 적용되도록 한다.
 */
@Component
public class PoliceFoundItemSaver {

	private static final Logger log = LoggerFactory.getLogger(PoliceFoundItemSaver.class);

	private final PoliceFoundItemMapper mapper;
	private final PoliceStationAddressResolver stationAddressResolver;
	private final FoundItemRepository foundItemRepository;
	private final PoliceFoundItemImageService imageService;
	private final ApplicationEventPublisher eventPublisher;

	public PoliceFoundItemSaver(
			PoliceFoundItemMapper mapper,
			PoliceStationAddressResolver stationAddressResolver,
			FoundItemRepository foundItemRepository,
			PoliceFoundItemImageService imageService,
			ApplicationEventPublisher eventPublisher
	) {
		this.mapper = mapper;
		this.stationAddressResolver = stationAddressResolver;
		this.foundItemRepository = foundItemRepository;
		this.imageService = imageService;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 신규 습득물이면 저장하고, 이미 존재하면 업데이트한다.
	 * 각 item이 독립 트랜잭션으로 처리되어 한 건 실패가 전체에 영향을 주지 않는다.
	 *
	 * @return 신규 저장이면 true, 이미 존재하거나 실패하면 false
	 */
	@Transactional
	public boolean saveIfNew(
			PoliceFoundItemResponse response,
			Optional<PoliceFoundItemDetailResponse> detail,
			String regionName
	) {
		if (response == null || !StringUtils.hasText(response.atcId())) {
			return false;
		}

		String atcId = response.atcId().trim();
		Integer fdSn = mapper.parseOptionalFdSn(response.fdSn());
		if (fdSn == null) {
			return false;
		}

		Optional<FoundItem> existingItem = foundItemRepository.findByAtcIdAndFdSn(atcId, fdSn);
		if (existingItem.isPresent()) {
			updateExisting(existingItem.get(), response, detail, regionName);
			return false;
		}

		try {
			String foundArea = detail
					.flatMap(detailResponse -> stationAddressResolver.resolveFoundArea(detailResponse, regionName))
					.orElse(regionName);
			FoundItem foundItem = mapper.toFoundItem(response, detail.orElse(null), foundArea);
			FoundItem savedItem = foundItemRepository.save(foundItem);
			if (detail.isPresent()) {
				imageService.saveImageIfPresent(savedItem, detail.get());
			} else {
				imageService.saveImageIfPresent(savedItem, response);
			}
			eventPublisher.publishEvent(new FoundItemMatchRequestedEvent(savedItem.getFoundId()));
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("이미 저장된 경찰청 습득물입니다. atcId={}, fdSn={}", atcId, fdSn, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("경찰청 습득물 응답을 저장하지 못했습니다. atcId={}, fdSn={}, reason={}", atcId, fdSn, exception.getMessage());
			return false;
		}
	}

	private void updateExisting(
			FoundItem existingItem,
			PoliceFoundItemResponse response,
			Optional<PoliceFoundItemDetailResponse> detail,
			String regionName
	) {
		String foundArea = detail
				.flatMap(detailResponse -> stationAddressResolver.resolveFoundArea(detailResponse, regionName))
				.orElse(regionName);
		FoundItem mappedItem = mapper.toFoundItem(response, detail.orElse(null), foundArea);
		existingItem.updatePoliceDetail(
				mappedItem.getTitle(),
				detail.isPresent() ? mappedItem.getContent() : existingItem.getContent(),
				mappedItem.getItemName(),
				mappedItem.getCategoryMain(),
				mappedItem.getCategorySub(),
				mappedItem.getColorName(),
				mappedItem.getFoundAt(),
				mappedItem.getFoundArea(),
				mappedItem.getFoundPlace(),
				detail.isPresent() ? mappedItem.getKeepPlace() : existingItem.getKeepPlace(),
				detail.isPresent() ? mappedItem.getContact() : existingItem.getContact(),
				detail.isPresent() ? mappedItem.getCustodyStatus() : existingItem.getCustodyStatus(),
				detail.isPresent() ? mappedItem.getReceiveType() : existingItem.getReceiveType(),
				mappedItem.getStatus()
		);
	}
}
