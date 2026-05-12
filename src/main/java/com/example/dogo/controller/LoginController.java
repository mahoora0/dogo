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
import com.example.dogo.service.ProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.dogo.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequiredArgsConstructor
public class LoginController {
    
  private final UserRepository userRepository;
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
  public String withdraw(@AuthenticationPrincipal CustomUserDetails userDetails,
                         HttpServletRequest request) throws Exception {
    com.example.dogo.entity.User user = userDetails.getUser();
    
    // 상태를 'WITHDRAWN'으로 변경 (또는 삭제 userRepository.delete(user))
    // 여기서는 영속성 컨텍스트 관리를 위해 레포지토리를 통해 명시적으로 업데이트하거나 로직 수행
    com.example.dogo.entity.User dbUser = userRepository.findById(user.getUserNo()).orElseThrow();
    
    // User 엔티티에 status 필드를 변경할 수 있는 메서드가 없으면 리플렉션을 쓰거나 필드 추가 필요
    // 일단 간단히 엔티티 직접 수정을 고려하여 status 변경 로직 추가 (User.java 수정 필요할수도 있음)
    // 여기서는 예시로 soft delete 또는 hard delete 처리
    userRepository.delete(dbUser);
    
    // 로그아웃 처리
    request.logout();
    
    return "redirect:/";
  }
}
