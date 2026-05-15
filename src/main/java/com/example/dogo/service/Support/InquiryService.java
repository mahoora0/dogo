package com.example.dogo.service.Support;

import com.example.dogo.entity.Support.Inquiry;
import com.example.dogo.entity.Support.InquiryFile;
import com.example.dogo.entity.user.User;
import com.example.dogo.repository.Support.InquiryFileRepository;
import com.example.dogo.repository.Support.InquiryRepository;
import com.example.dogo.repository.user.UserRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InquiryService {

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
    public Page<InquirySummary> getInquiryPage(String viewMode, String status, int page, int size, User user, boolean isAdmin) {
        Pageable pageable = PageRequest.of(page, size);
        
        boolean hasStatus = StringUtils.hasText(status) && !"전체".equals(status);
        Page<Inquiry> inquiryPage;

        // 1. 내 문의 목록 보기 (viewMode == "my")
        if ("my".equals(viewMode)) {
            if (user == null) return Page.empty(); 
            
            if (hasStatus) {
                if ("답변완료".equals(status)) {
                    inquiryPage = inquiryRepository.findByUserAndStatusOrderByRegdateDescInquiryIdDesc(user, "ANSWERED", pageable);
                } else {
                    inquiryPage = inquiryRepository.findByUserAndStatusNotOrderByRegdateDescInquiryIdDesc(user, "ANSWERED", pageable);
                }
            } else {
                inquiryPage = inquiryRepository.findByUserOrderByRegdateDescInquiryIdDesc(user, pageable);
            }
        } 
        // 2. 전체 보기 (viewMode == "all") - 비로그인 시에도 여기서 전체를 가져와야 함
        else {
            if (hasStatus) {
                if ("답변완료".equals(status)) {
                    inquiryPage = inquiryRepository.findByStatusOrderByRegdateDescInquiryIdDesc("ANSWERED", pageable);
                } else {
                    inquiryPage = inquiryRepository.findByStatusNotOrderByRegdateDescInquiryIdDesc("ANSWERED", pageable);
                }
            } else {
                inquiryPage = inquiryRepository.findAllByOrderByRegdateDescInquiryIdDesc(pageable);
            }
        }

        return inquiryPage.map(inquiry -> toSummaryWithPrivacy(inquiry, user, isAdmin));
    }

    @Transactional(readOnly = true)
    public InquiryDetail getInquiryDetail(Long id, User currentUser, boolean isAdmin) {
        Inquiry inquiry = getInquiry(id);
        
        // 권한 체크 시에도 User가 null일 경우를 대비함
        boolean isOwner = currentUser != null && inquiry.getUser() != null && 
                          inquiry.getUser().getUserNo().equals(currentUser.getUserNo());
        
        if (!isAdmin && !isOwner) {
            throw new IllegalArgumentException("이 문의사항은 비밀글입니다. 작성자 본인만 확인할 수 있습니다.");
        }
        
        return toDetail(inquiry);
    }

    @Transactional
    public void create(String category, String title, String content, List<MultipartFile> files, User user) {
        if (user == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }
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

    private InquirySummary toSummaryWithPrivacy(Inquiry inquiry, User currentUser, boolean isAdmin) {
        String createdAt = inquiry.getRegdate() == null ? "" : inquiry.getRegdate().format(CREATED_AT_FORMATTER);
        
        // 작성자 본인 여부 확인 (User가 null인 경우도 안전하게 처리)
        boolean isOwner = currentUser != null && inquiry.getUser() != null && 
                          inquiry.getUser().getUserNo().equals(currentUser.getUserNo());
        
        // 비밀글 여부: 관리자가 아니고, 본인 글도 아닌 경우
        boolean isSecret = !isAdmin && !isOwner;
        
        String displayTitle = isSecret ? "비밀글입니다." : inquiry.getTitle();

        return new InquirySummary(
                inquiry.getInquiryId(),
                inquiry.getCategory(),
                categoryLabel(inquiry.getCategory()),
                displayTitle,
                inquiry.getContent(),
                createdAt,
                statusLabel(inquiry.getStatus()),
                isSecret
        );
    }

    private InquiryDetail toDetail(Inquiry inquiry) {
        String createdAt = inquiry.getRegdate() == null ? "" : inquiry.getRegdate().format(CREATED_AT_FORMATTER);
        String answeredAt = inquiry.getAnsweredAt() == null ? "" : inquiry.getAnsweredAt().format(CREATED_AT_FORMATTER);

        List<InquiryFileView> files = inquiryFileRepository.findByInquiryOrderByFileIdAsc(inquiry).stream()
                .map(f -> new InquiryFileView(f.getFileId(), f.getOriginalName(), f.getFileUrl(), f.getFileSize()))
                .toList();

        String writerEmail = inquiry.getUser() == null ? "" : inquiry.getUser().getEmail();

        return new InquiryDetail(
                inquiry.getInquiryId(),
                categoryLabel(inquiry.getCategory()),
                inquiry.getTitle(),
                inquiry.getContent(),
                statusLabel(inquiry.getStatus()),
                inquiry.getAnswer(),
                createdAt,
                answeredAt,
                files,
                writerEmail
        );
    }

    private AdminInquiryRow toAdminRow(Inquiry inquiry) {
        Long userId = inquiry.getUser() == null ? null : inquiry.getUser().getUserNo();
        return new AdminInquiryRow(inquiry.getInquiryId(), inquiry.getTitle(), userId, statusLabel(inquiry.getStatus()));
    }

    private AdminInquiryDetail toAdminDetail(Inquiry inquiry) {
        Long userId = inquiry.getUser() == null ? null : inquiry.getUser().getUserNo();
        String createdAt = inquiry.getRegdate() == null ? "" : inquiry.getRegdate().format(CREATED_AT_FORMATTER);
        String answeredAt = inquiry.getAnsweredAt() == null ? "" : inquiry.getAnsweredAt().format(CREATED_AT_FORMATTER);

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
        if (category == null) return "기타 문의";
        return switch (category) {
            case "분실물" -> "분실물 문의";
            case "습득물" -> "습득물 문의";
            case "서비스" -> "서비스 문의";
            default -> "기타 문의";
        };
    }

    private String statusLabel(String status) {
        if (status == null) return "접수";
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
            String status,
            boolean isSecret
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
            List<InquiryFileView> files,
            String writerEmail
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
