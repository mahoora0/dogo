package com.example.dogo.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProfileService {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null; // 이미지가 없으면 null 반환
        }

        try {
            // 업로드 디렉토리가 없으면 생성
            Path uploadPath = Paths.get(uploadDir, "profiles").toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 고유한 파일명 생성 (충돌 방지)
            String originalFilename = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
            String extension = extractImageExtension(originalFilename, file.getContentType());
            String storedFileName = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(storedFileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new IllegalArgumentException("올바르지 않은 프로필 이미지 파일명입니다.");
            }
            file.transferTo(filePath.toFile());

            // 웹 접근 경로 반환 (WebConfig에서 매핑할 경로)
            return "/uploads/profiles/" + storedFileName;

        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 저장에 실패했습니다.", e);
        }
    }

    private String extractImageExtension(String filename, String contentType) {
        if (!StringUtils.hasText(filename) || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("올바르지 않은 프로필 이미지 파일명입니다.");
        }

        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex < 0) {
            throw new IllegalArgumentException("프로필 이미지는 지원되는 이미지 파일이어야 합니다.");
        }

        String extension = filename.substring(extensionIndex).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 프로필 이미지 형식입니다.");
        }
        if (!StringUtils.hasText(contentType) || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("지원하지 않는 프로필 이미지 형식입니다.");
        }
        return extension;
    }
}
