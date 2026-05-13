package com.example.dogo.dto.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserJoinDto {
    private String username; // form의 name="username"과 매핑 (LOGIN_ID로 사용)
    private String password;
    private String passwordConfirm;
    private String email;
    private String phone;
    private MultipartFile profileImage;
}
