package com.example.dogo.service.upload;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public final class UploadFileValidator {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> BLOCKED_ATTACHMENT_EXTENSIONS = Set.of(
            ".html", ".htm", ".svg", ".xml", ".xhtml", ".js", ".mjs", ".css"
    );
    private static final Set<String> BLOCKED_ATTACHMENT_CONTENT_TYPES = Set.of(
            "text/html", "image/svg+xml", "application/xml", "text/xml", "application/xhtml+xml",
            "text/javascript", "application/javascript", "text/css"
    );

    private UploadFileValidator() {
    }

    public static String imageExtension(MultipartFile file) {
        String extension = extension(file);
        String contentType = normalizedContentType(file);
        if (!IMAGE_EXTENSIONS.contains(extension) || !IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }
        return extension;
    }

    public static String attachmentExtension(MultipartFile file) {
        String extension = extension(file);
        String contentType = normalizedContentType(file);
        if (BLOCKED_ATTACHMENT_EXTENSIONS.contains(extension) || BLOCKED_ATTACHMENT_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("업로드할 수 없는 파일 형식입니다.");
        }
        return extension;
    }

    private static String extension(MultipartFile file) {
        String filename = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
        if (!StringUtils.hasText(filename) || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex < 0) {
            throw new IllegalArgumentException("파일 확장자가 필요합니다.");
        }
        String extension = filename.substring(extensionIndex).toLowerCase(Locale.ROOT);
        if (extension.length() > 12) {
            throw new IllegalArgumentException("올바르지 않은 파일 확장자입니다.");
        }
        return extension;
    }

    private static String normalizedContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return StringUtils.hasText(contentType) ? contentType.toLowerCase(Locale.ROOT) : "";
    }
}
