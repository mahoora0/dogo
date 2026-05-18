package com.example.dogo.service.Support;

import com.example.dogo.entity.Support.Guide;
import com.example.dogo.repository.Support.GuideRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import com.example.dogo.entity.Support.GuideImage;
import com.example.dogo.repository.Support.GuideImageRepository;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GuideService {

    private final GuideRepository guideRepository;
    private final GuideImageRepository guideImageRepository;
    private final Path guideUploadPath;

    public GuideService(GuideRepository guideRepository, GuideImageRepository guideImageRepository, @Value("${file.upload-dir}") String uploadDir) {
        this.guideRepository = guideRepository;
        this.guideImageRepository = guideImageRepository;
        this.guideUploadPath = Path.of(uploadDir, "guides").toAbsolutePath().normalize();
    }

    public Page<Guide> getGuides(String category, int page, int size) {
        Pageable pageable = Path.of("").toAbsolutePath().toString().contains("test") ? null : PageRequest.of(page, size);
        Pageable realPageable = PageRequest.of(page, size);
        if (category == null || category.isEmpty() || "전체".equals(category)) {
            return guideRepository.findByDeletedOrderByCreatedAtDesc("N", realPageable);
        }
        return guideRepository.findByCategoryAndDeletedOrderByCreatedAtDesc(category, "N", realPageable);
    }

    public Guide getGuideDetail(Long id) {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이용가이드입니다. ID: " + id));
        
        if ("Y".equals(guide.getDeleted())) {
            throw new IllegalArgumentException("삭제된 이용가이드입니다. ID: " + id);
        }

        return guide;
    }

    @Transactional
    public Guide createGuide(String title, String category, String content, List<MultipartFile> images) {
        Guide guide = Guide.builder()
                .title(title)
                .category(category)
                .content(content)
                .deleted("N")
                .build();
        
        // Save guide first to obtain generated ID
        Guide savedGuide = guideRepository.save(guide);

        if (images != null && !images.isEmpty()) {
            int order = 0;
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String storedPath = saveImageFile(image);
                    GuideImage guideImage = GuideImage.builder()
                            .guide(savedGuide)
                            .imagePath(storedPath)
                            .ortOrder(order++)
                            .build();
                    // Explicitly persist GuideImage to avoid Transient reference issues
                    guideImageRepository.save(guideImage);
                    savedGuide.addGuideImage(guideImage);
                }
            }
        }

        return savedGuide;
    }

    @Transactional
    public Guide updateGuide(Long id, String title, String category, String content, List<MultipartFile> images) {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이용가이드입니다. ID: " + id));
        guide.setTitle(title);
        guide.setCategory(category);
        guide.setContent(content);

        // Check if there's at least one non-empty file uploaded
        boolean hasNewImages = false;
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    hasNewImages = true;
                    break;
                }
            }
        }

        if (hasNewImages) {
            // Remove existing guide images from the collection
            guide.getGuideImages().clear();
            
            int order = 0;
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String storedPath = saveImageFile(image);
                    GuideImage guideImage = GuideImage.builder()
                            .guide(guide)
                            .imagePath(storedPath)
                            .ortOrder(order++)
                            .build();
                    // Explicitly persist GuideImage to avoid Transient reference issues
                    guideImageRepository.save(guideImage);
                    guide.addGuideImage(guideImage);
                }
            }
        }

        return guide;
    }

    @Transactional
    public void deleteGuide(Long id) {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이용가이드입니다. ID: " + id));
        guide.setDeleted("Y");
    }

    private String saveImageFile(MultipartFile file) {
        try {
            Files.createDirectories(guideUploadPath);

            String originalName = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
            String extension = extractExtension(originalName);
            String storedName = UUID.randomUUID() + extension;
            Path targetPath = guideUploadPath.resolve(storedName).normalize();
            
            if (!targetPath.startsWith(guideUploadPath)) {
                throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
            }

            file.transferTo(targetPath);
            return "/uploads/guides/" + storedName;
        } catch (IOException exception) {
            throw new UncheckedIOException("이미지 저장에 실패했습니다.", exception);
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (extension.length() > 12) {
            return "";
        }
        return extension;
    }
}
