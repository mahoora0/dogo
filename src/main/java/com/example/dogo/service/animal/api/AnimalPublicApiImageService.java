package com.example.dogo.service.animal.api;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportImage;
import com.example.dogo.repository.animal.AnimalReportImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AnimalPublicApiImageService {

	private final AnimalReportImageRepository imageRepository;

	public AnimalPublicApiImageService(AnimalReportImageRepository imageRepository) {
		this.imageRepository = imageRepository;
	}

	public void saveExternalImageIfPresent(AnimalReport report, String imageUrl) {
		if (report == null || !StringUtils.hasText(imageUrl)) {
			return;
		}
		imageRepository.save(new AnimalReportImage(
				report,
				imageUrl,
				imageUrl,
				imageUrl,
				"image/external",
				0L,
				0
		));
	}
}
