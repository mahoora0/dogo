package com.example.dogo.service.police.sync;

import com.example.dogo.dto.police.PoliceLostItemDetailResponse;
import com.example.dogo.dto.police.PoliceLostItemResponse;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.service.match.LostItemMatchRequestedEvent;
import com.example.dogo.service.police.mapper.PoliceLostItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 경찰청 분실물 단건 저장 컴포넌트.
 * saveIfNew를 별도 빈으로 분리하여 Spring AOP 프록시를 통해
 * REQUIRES_NEW 트랜잭션이 올바르게 적용되도록 한다.
 */
@Component
public class PoliceLostItemSaver {

	private static final Logger log = LoggerFactory.getLogger(PoliceLostItemSaver.class);

	private final PoliceLostItemMapper mapper;
	private final LostItemRepository lostItemRepository;
	private final PoliceLostItemImageService imageService;
	private final ApplicationEventPublisher eventPublisher;

	public PoliceLostItemSaver(
			PoliceLostItemMapper mapper,
			LostItemRepository lostItemRepository,
			PoliceLostItemImageService imageService,
			ApplicationEventPublisher eventPublisher
	) {
		this.mapper = mapper;
		this.lostItemRepository = lostItemRepository;
		this.imageService = imageService;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 신규 분실물이면 저장하고, 이미 존재하면 업데이트한다.
	 * 각 item이 독립 트랜잭션으로 처리되어 한 건 실패가 전체에 영향을 주지 않는다.
	 *
	 * @return 신규 저장이면 true, 이미 존재하거나 실패하면 false
	 */
	@Transactional
	public boolean saveIfNew(
			PoliceLostItemResponse response,
			boolean includeDetail,
			Optional<PoliceLostItemDetailResponse> detail
	) {
		if (response == null || !StringUtils.hasText(response.atcId())) {
			return false;
		}

		String atcId = response.atcId().trim();
		Optional<LostItem> existingItem = lostItemRepository.findByAtcId(atcId);
		if (existingItem.isPresent()) {
			updateExisting(existingItem.get(), response, detail);
			return false;
		}

		try {
			LostItem lostItem = mapper.toLostItem(response, detail.orElse(null));
			LostItem savedItem = lostItemRepository.save(lostItem);
			detail.ifPresent(detailResponse -> imageService.saveImageIfPresent(savedItem, detailResponse));
			eventPublisher.publishEvent(new LostItemMatchRequestedEvent(savedItem.getLostId()));
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("이미 저장된 경찰청 분실물입니다. atcId={}", atcId, exception);
			return false;
		} catch (IllegalArgumentException exception) {
			log.warn("경찰청 분실물 응답을 저장하지 못했습니다. atcId={}, reason={}", atcId, exception.getMessage());
			return false;
		}
	}

	private void updateExisting(
			LostItem existingItem,
			PoliceLostItemResponse response,
			Optional<PoliceLostItemDetailResponse> detail
	) {
		LostItem mappedItem = mapper.toLostItem(response, detail.orElse(null));
		existingItem.updatePoliceDetail(
				mappedItem.getTitle(),
				detail.isPresent() ? mappedItem.getContent() : existingItem.getContent(),
				mappedItem.getItemName(),
				mappedItem.getCategoryMain(),
				mappedItem.getCategorySub(),
				mappedItem.getColorName(),
				mappedItem.getLostAt(),
				mappedItem.getLostArea(),
				mappedItem.getLostPlace(),
				detail.isPresent() ? mappedItem.getContact() : existingItem.getContact()
		);
	}
}
