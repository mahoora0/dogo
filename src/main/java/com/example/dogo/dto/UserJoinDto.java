package com.example.dogo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserJoinDto {
    private String loginId;
    private String nickname;
    private String password;
    private String passwordConfirm;
    private String email;
    private MultipartFile profileImage;
}
