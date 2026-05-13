package com.example.dogo.service;

import com.example.dogo.entity.Inquiry;
import com.example.dogo.entity.User;
import com.example.dogo.repository.InquiryRepository;
import com.example.dogo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InquiryService {

    private static final String DEV_USER_EMAIL = "dev@dogo.local";
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    public InquiryService(InquiryRepository inquiryRepository, UserRepository userRepository) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, List<InquirySummary>> groupedInquiries() {
        Map<String, List<InquirySummary>> groups = new LinkedHashMap<>();
        groups.put("분실물 문의", inquiriesByCategory("분실물"));
        groups.put("습득물 문의", inquiriesByCategory("습득물"));
        groups.put("서비스 문의", inquiriesByCategory("서비스"));
        groups.put("기타 문의", inquiriesByCategory("기타"));
        return groups;
    }

    @Transactional(readOnly = true)
    public List<InquirySummary> getInquiries() {
        return inquiryRepository.findAllByOrderByRegdateDescInquiryIdDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryDetail getInquiryDetail(Long id) {
        return toDetail(getInquiry(id));
    }

    @Transactional
    public void create(String category, String title, String content) {
        User user = getOrCreateDevUser();
        inquiryRepository.save(new Inquiry(user, category, title, content));
    }

    @Transactional(readOnly = true)
    public List<AdminInquiryRow> getAdminInquiries() {
        return inquiryRepository.findAllByOrderByRegdateDescInquiryIdDesc().stream()
                .map(this::toAdminRow)
                .toList();
    }

    @Transactional
    public AdminInquiryDetail getAdminInquiryDetail(Long id) {
        Inquiry inquiry = getInquiry(id);
        inquiry.markChecking();
        return toAdminDetail(inquiry);
    }

    @Transactional
    public void answer(Long id, String answer) {
        Inquiry inquiry = getInquiry(id);
        inquiry.answer(answer);
    }

    private List<InquirySummary> inquiriesByCategory(String category) {
        return inquiryRepository.findByCategoryOrderByRegdateDescInquiryIdDesc(category).stream()
                .map(this::toSummary)
                .toList();
    }

    private Inquiry getInquiry(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
    }

    private InquirySummary toSummary(Inquiry inquiry) {
        String createdAt = inquiry.getRegdate() == null
                ? ""
                : inquiry.getRegdate().format(CREATED_AT_FORMATTER);

        return new InquirySummary(
                inquiry.getInquiryId(),
                inquiry.getCategory(),
                categoryLabel(inquiry.getCategory()),
                inquiry.getTitle(),
                inquiry.getContent(),
                createdAt,
                statusLabel(inquiry.getStatus())
        );
    }

    private InquiryDetail toDetail(Inquiry inquiry) {
        String createdAt = inquiry.getRegdate() == null
                ? ""
                : inquiry.getRegdate().format(CREATED_AT_FORMATTER);
        String answeredAt = inquiry.getAnsweredAt() == null
                ? ""
                : inquiry.getAnsweredAt().format(CREATED_AT_FORMATTER);

        return new InquiryDetail(
                inquiry.getInquiryId(),
                categoryLabel(inquiry.getCategory()),
                inquiry.getTitle(),
                inquiry.getContent(),
                statusLabel(inquiry.getStatus()),
                inquiry.getAnswer(),
                createdAt,
                answeredAt
        );
    }

    private User getOrCreateDevUser() {
        return userRepository.findByEmail(DEV_USER_EMAIL)
                .orElseGet(() -> userRepository.save(new User(DEV_USER_EMAIL, "개발용 사용자", "010-0000-0000")));
    }

    private AdminInquiryRow toAdminRow(Inquiry inquiry) {
        Long userId = inquiry.getUser() == null ? null : inquiry.getUser().getUserNo();

        return new AdminInquiryRow(
                inquiry.getInquiryId(),
                inquiry.getTitle(),
                userId,
                statusLabel(inquiry.getStatus())
        );
    }

    private AdminInquiryDetail toAdminDetail(Inquiry inquiry) {
        Long userId = inquiry.getUser() == null ? null : inquiry.getUser().getUserNo();
        String createdAt = inquiry.getRegdate() == null
                ? ""
                : inquiry.getRegdate().format(CREATED_AT_FORMATTER);
        String answeredAt = inquiry.getAnsweredAt() == null
                ? ""
                : inquiry.getAnsweredAt().format(CREATED_AT_FORMATTER);

        return new AdminInquiryDetail(
                inquiry.getInquiryId(),
                inquiry.getCategory(),
                categoryLabel(inquiry.getCategory()),
                inquiry.getTitle(),
                inquiry.getContent(),
                userId,
                statusLabel(inquiry.getStatus()),
                inquiry.getAnswer(),
                createdAt,
                answeredAt
        );
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "분실물" -> "분실물 문의";
            case "습득물" -> "습득물 문의";
            case "서비스" -> "서비스 문의";
            default -> "기타 문의";
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "CHECKING" -> "확인중";
            case "ANSWERED" -> "답변완료";
            default -> "접수";
        };
    }

    public record InquirySummary(
            Long id,
            String category,
            String categoryLabel,
            String title,
            String content,
            String createdAt,
            String status
    ) {
    }

    public record InquiryDetail(
            Long id,
            String categoryLabel,
            String title,
            String content,
            String status,
            String answer,
            String createdAt,
            String answeredAt
    ) {
    }

    public record AdminInquiryRow(
            Long id,
            String title,
            Long userId,
            String status
    ) {
    }

    public record AdminInquiryDetail(
            Long id,
            String category,
            String categoryLabel,
            String title,
            String content,
            Long userId,
            String status,
            String answer,
            String createdAt,
            String answeredAt
    ) {
    }
}
