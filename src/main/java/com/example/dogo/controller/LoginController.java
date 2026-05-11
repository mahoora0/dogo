package com.example.dogo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class LoginController {

  @GetMapping("/login")
  public String loginPage() {
    return "user/login"; // templates/user/login.html을 찾아감
  }

  @GetMapping("/join")
  public String joinPage() {
    return "user/join"; // templates/user/join.html을 찾아감
  }

  @GetMapping("/userpage")
  public String userPage() {
    return "user/userpage"; // templates/user/userpage.html을 찾아감
  }

  @PostMapping("/join")
  public String joinProcess(@ModelAttribute com.example.dogo.dto.UserJoinDto userJoinDto, 
                            com.example.dogo.repository.UserRepository userRepository,
                            com.example.dogo.service.ProfileService profileService,
                            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
    
    // 프로필 이미지 저장
    String profileImageUrl = profileService.saveProfileImage(userJoinDto.getProfileImage());

    // 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(userJoinDto.getPassword());

    // 유저 생성 및 저장
    com.example.dogo.entity.User newUser = new com.example.dogo.entity.User(
            userJoinDto.getUsername(),
            encodedPassword,
            userJoinDto.getEmail(),
            userJoinDto.getUsername(), // 닉네임이 입력폼에 없으므로 아이디로 임시 대체 (필요시 폼에 닉네임 추가)
            userJoinDto.getPhone(),
            profileImageUrl
    );
    userRepository.save(newUser);

    return "redirect:/login";
  }
}