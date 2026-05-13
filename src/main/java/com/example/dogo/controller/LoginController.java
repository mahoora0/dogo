package com.example.dogo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import com.example.dogo.repository.UserRepository;
import com.example.dogo.repository.UserSocialAccountRepository;
import com.example.dogo.service.ProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.dogo.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequiredArgsConstructor
public class LoginController {
    
  private final UserRepository userRepository;
  private final UserSocialAccountRepository userSocialAccountRepository;
  private final ProfileService profileService;
  private final PasswordEncoder passwordEncoder;

  @GetMapping("/login")
  public String loginPage() {
    return "user/login"; // templates/user/login.html을 찾아감
  }

  @GetMapping("/join")
  public String joinPage() {
    return "user/join"; // templates/user/join.html을 찾아감
  }

  @GetMapping("/api/user/check-nickname")
  @ResponseBody
  public ResponseEntity<Boolean> checkNickname(@RequestParam("nickname") String nickname) {
    boolean exists = userRepository.existsByNickname(nickname);
    return ResponseEntity.ok(exists);
  }

  @GetMapping("/userpage")
  public String userPage() {
    return "user/userpage"; // templates/user/userpage.html을 찾아감
  }

  @PostMapping("/join")
  public String joinProcess(@ModelAttribute com.example.dogo.dto.UserJoinDto userJoinDto) {

    // 프로필 이미지 저장
    String profileImageUrl = profileService.saveProfileImage(userJoinDto.getProfileImage());

    // 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(userJoinDto.getPassword());

    com.example.dogo.entity.User newUser = new com.example.dogo.entity.User(
            userJoinDto.getLoginId(),
            encodedPassword,
            userJoinDto.getEmail(),
            userJoinDto.getNickname(),
            userJoinDto.getPhone(),
            profileImageUrl
    );
    userRepository.save(newUser);

    return "redirect:/login";
  }

  @PostMapping("/user/withdraw")
  @Transactional
  public String withdraw(@AuthenticationPrincipal CustomUserDetails userDetails,
                         HttpServletRequest request) throws Exception {
    com.example.dogo.entity.User user = userDetails.getUser();
    
    com.example.dogo.entity.User dbUser = userRepository.findById(user.getUserNo()).orElseThrow();
    
    // 소셜 계정 연결 정보가 있다면 삭제 (다음에 다시 동의창을 띄우기 위함)
    userSocialAccountRepository.deleteByUser(dbUser);
    
    // Hard Delete 대신 Soft Delete (상태 변경) 처리
    // 다른 테이블(소셜 계정, 게시글 등)과의 외래 키 제약 조건을 피하기 위함입니다.
    dbUser.withdraw();
    
    // 로그아웃 처리
    request.logout();
    
    return "redirect:/";
  }
}
