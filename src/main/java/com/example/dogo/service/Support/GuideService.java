package com.example.dogo.service.Support;

import com.example.dogo.entity.Support.Guide;
import com.example.dogo.repository.Support.GuideRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GuideService {

    private final GuideRepository guideRepository;

    public GuideService(GuideRepository guideRepository) {
        this.guideRepository = guideRepository;
    }

    public Page<Guide> getGuides(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (category == null || category.isEmpty() || "전체".equals(category)) {
            return guideRepository.findByDeletedOrderByCreatedAtDesc("N", pageable);
        }
        return guideRepository.findByCategoryAndDeletedOrderByCreatedAtDesc(category, "N", pageable);
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
    public Guide createGuide(String title, String category, String content, String writer) {
        Guide guide = Guide.builder()
                .title(title)
                .category(category)
                .content(content)
                .writer(writer == null || writer.isEmpty() ? "관리자" : writer)
                .deleted("N")
                .build();
        return guideRepository.save(guide);
    }

    @Transactional
    public Guide updateGuide(Long id, String title, String category, String content) {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이용가이드입니다. ID: " + id));
        guide.setTitle(title);
        guide.setCategory(category);
        guide.setContent(content);
        return guide;
    }

    @Transactional
    public void deleteGuide(Long id) {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이용가이드입니다. ID: " + id));
        guide.setDeleted("Y");
    }
}
