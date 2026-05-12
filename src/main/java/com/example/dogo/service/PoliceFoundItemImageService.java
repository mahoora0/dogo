package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemResponse;
import com.example.dogo.entity.FoundItem;
import com.example.dogo.entity.FoundItemImage;
import com.example.dogo.repository.FoundItemImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class PoliceFoundItemImageService {

	private final FoundItemImageRepository foundItemImageRepository;

	public PoliceFoundItemImageService(FoundItemImageRepository foundItemImageRepository) {
		this.foundItemImageRepository = foundItemImageRepository;
	}

	public void saveImageIfPresent(FoundItem foundItem, PoliceFoundItemResponse response) {
		saveImageIfPresent(foundItem, response == null ? null : response.fdFilePathImg());
	}

	public void saveImageIfPresent(FoundItem foundItem, PoliceFoundItemDetailResponse detail) {
		saveImageIfPresent(foundItem, detail == null ? null : detail.fdFilePathImg());
	}

	private void saveImageIfPresent(FoundItem foundItem, String imageUrl) {
		if (!isActualImageUrl(imageUrl)) {
			return;
		}
		if (foundItemImageRepository.findFirstByFoundItemOrderBySortOrderAscImageIdAsc(foundItem).isPresent()) {
			return;
		}

		foundItemImageRepository.save(new FoundItemImage(
				foundItem,
				originalName(imageUrl),
				foundItem.getAtcId(),
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
			return "police-found-image";
		}
		return "police-found-image";
	}
}
