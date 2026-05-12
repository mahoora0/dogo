package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.entity.LostItem;
import com.example.dogo.entity.LostItemImage;
import com.example.dogo.repository.LostItemImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class PoliceLostItemImageService {

	private final LostItemImageRepository lostItemImageRepository;

	public PoliceLostItemImageService(LostItemImageRepository lostItemImageRepository) {
		this.lostItemImageRepository = lostItemImageRepository;
	}

	public void saveImageIfPresent(LostItem lostItem, PoliceLostItemDetailResponse detail) {
		String imageUrl = detail.lstFilePathImg();
		if (!isActualImageUrl(imageUrl)) {
			return;
		}
		if (lostItemImageRepository.findFirstByLostItemOrderBySortOrderAscImageIdAsc(lostItem).isPresent()) {
			return;
		}

		lostItemImageRepository.save(new LostItemImage(
				lostItem,
				originalName(imageUrl),
				lostItem.getAtcId(),
				imageUrl.trim(),
				"image/external",
				null,
				0
		));
	}

	private boolean isActualImageUrl(String imageUrl) {
		if (!StringUtils.hasText(imageUrl)) {
			return false;
		}

		String normalized = imageUrl.trim().toLowerCase();
		return normalized.startsWith("http")
				&& !normalized.contains("no_img")
				&& !normalized.contains("noimage");
	}

	private String originalName(String imageUrl) {
		try {
			String path = new URI(imageUrl.trim()).getPath();
			if (StringUtils.hasText(path) && path.contains("/")) {
				String filename = path.substring(path.lastIndexOf('/') + 1);
				if (StringUtils.hasText(filename)) {
					return filename;
				}
			}
		} catch (URISyntaxException ignored) {
			return "police-image";
		}
		return "police-image";
	}
}
