package com.example.dogo.service;

import com.example.dogo.entity.Inquiry;
import com.example.dogo.entity.InquiryFile;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.InquiryFileRepository;
import com.example.dogo.repository.InquiryRepository;
import com.example.dogo.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class InquiryService {

    private static final String DEV_USER_EMAIL = "dev@dogo.local";
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final InquiryRepository inquiryRepository;
    private final InquiryFileRepository inquiryFileRepository;
    private final UserRepository userRepository;
    private final Path inquiryUploadPath;

    public InquiryService(
            InquiryRepository inquiryRepository, 
            InquiryFileRepository inquiryFileRepository,
            UserRepository userRepository,
            @Value("${file.upload-dir}") String uploadDir) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryFileRepository = inquiryFileRepository;
        this.userRepository = userRepository;
        this.inquiryUploadPath = Path.of(uploadDir, "inquiries").toAbsolutePath().normalize();
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
    public void create(String category, String title, String content, List<MultipartFile> files) {
        User user = getOrCreateDevUser();
        Inquiry savedInquiry = inquiryRepository.save(new Inquiry(user, category, title, content));
        
        if (files != null && !files.isEmpty()) {
            saveFiles(savedInquiry, files);
        }
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

    private void saveFiles(Inquiry inquiry, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            try {
                Files.createDirectories(inquiryUploadPath);

                String originalName = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
                String extension = extractExtension(originalName);
                String storedName = UUID.randomUUID() + extension;
                Path targetPath = inquiryUploadPath.resolve(storedName).normalize();
                
                if (!targetPath.startsWith(inquiryUploadPath)) {
                    throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
                }

                file.transferTo(targetPath);

                inquiryFileRepository.save(new InquiryFile(
                        inquiry,
                        originalName,
                        storedName,
                        "/uploads/inquiries/" + storedName,
                        file.getContentType(),
                        file.getSize()
                ));
            } catch (IOException exception) {
                throw new UncheckedIOException("파일 저장에 실패했습니다.", exception);
            }
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

        List<InquiryFileView> files = inquiryFileRepository.findByInquiryOrderByFileIdAsc(inquiry).stream()
                .map(f -> new InquiryFileView(f.getFileId(), f.getOriginalName(), f.getFileUrl(), f.getFileSize()))
                .toList();

        return new InquiryDetail(
                inquiry.getInquiryId(),
                categoryLabel(inquiry.getCategory()),
                inquiry.getTitle(),
                inquiry.getContent(),
                statusLabel(inquiry.getStatus()),
                inquiry.getAnswer(),
                createdAt,
                answeredAt,
                files
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
            String answeredAt,
            List<InquiryFileView> files
    ) {
    }

    public record InquiryFileView(
            Long fileId,
            String originalName,
            String fileUrl,
            Long fileSize
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
