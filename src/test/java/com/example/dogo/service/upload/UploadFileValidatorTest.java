package com.example.dogo.service.upload;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadFileValidatorTest {

    @Test
    void acceptsSupportedImageExtensionAndContentType() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

        assertEquals(".png", UploadFileValidator.imageExtension(file));
    }

    @Test
    void rejectsSvgImageUpload() {
        MockMultipartFile file = new MockMultipartFile("image", "attack.svg", "image/svg+xml", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> UploadFileValidator.imageExtension(file));
    }

    @Test
    void rejectsHtmlAttachmentUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "attack.html", "text/html", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> UploadFileValidator.attachmentExtension(file));
    }

    @Test
    void rejectsPathTraversalFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "../attack.png", "image/png", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> UploadFileValidator.imageExtension(file));
    }
}
