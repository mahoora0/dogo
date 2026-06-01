package com.example.dogo.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAllowedProfileImageInsideProfileDirectory() {
        ProfileService profileService = profileService();
        MockMultipartFile file = new MockMultipartFile("profileImage", "profile.png", "image/png", new byte[]{1, 2, 3});

        String imageUrl = profileService.saveProfileImage(file);

        assertTrue(imageUrl.startsWith("/uploads/profiles/"));
        assertTrue(imageUrl.endsWith(".png"));
    }

    @Test
    void rejectsPathTraversalInProfileImageFilename() {
        ProfileService profileService = profileService();
        MockMultipartFile file = new MockMultipartFile("profileImage", "profile.png/../../attack.png", "image/png", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> profileService.saveProfileImage(file));
    }

    @Test
    void rejectsSvgProfileImage() {
        ProfileService profileService = profileService();
        MockMultipartFile file = new MockMultipartFile("profileImage", "profile.svg", "image/svg+xml", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> profileService.saveProfileImage(file));
    }

    private ProfileService profileService() {
        ProfileService profileService = new ProfileService();
        ReflectionTestUtils.setField(profileService, "uploadDir", tempDir.toString());
        return profileService;
    }
}
