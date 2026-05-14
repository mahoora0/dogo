package com.example.dogo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserProfileUpdateDto {
    private String nickname;
    private String password;
    private String confirmPassword;
    private MultipartFile profileImage;
}
