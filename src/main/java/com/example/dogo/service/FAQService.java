package com.example.dogo.service;

import com.example.dogo.entity.FAQ;
import com.example.dogo.repository.FAQRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FAQService {

    private final FAQRepository faqRepository;

    public FAQService(FAQRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    public List<FAQ> getActiveFAQs() {
        return faqRepository.findAllByIsActiveTrueOrderBySortOrderAscIdDesc();
    }

    @Transactional
    public FAQ createFAQ(String category, String question, String answer) {
        FAQ faq = FAQ.builder()
                .category(category)
                .question(question)
                .answer(answer)
                .sortOrder(0)
                .isActive(true)
                .build();
        return faqRepository.save(faq);
    }

    @Transactional
    public void deleteFAQ(Long id) {
        faqRepository.deleteById(id);
    }
}
