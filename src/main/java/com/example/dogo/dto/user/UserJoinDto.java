package com.example.dogo.dto.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UserJoinDto {
    private String loginId; // form의 name="loginId"와 매핑 (아이디)
    private String nickname;
    private String password;
    private String passwordConfirm;
    private String email;
    private String emailVerificationToken;
    private MultipartFile profileImage;
}
