package com.example.dogo.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.repository.user.UserSocialAccountRepository;
import com.example.dogo.service.user.ProfileService;
import com.example.dogo.service.OAuth2Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.dogo.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.example.dogo.dto.UserProfileUpdateDto;
import org.springframework.util.StringUtils;

import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.item.ItemMatchRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.animal.AnimalReportMatchRepository;
import com.example.dogo.repository.Support.InquiryRepository;
import org.springframework.ui.Model;

@Controller
@RequiredArgsConstructor
public class LoginController {
    
  private final UserRepository userRepository;
  private final UserSocialAccountRepository userSocialAccountRepository;
  private final ProfileService profileService;
  private final PasswordEncoder passwordEncoder;
  private final LostItemRepository lostItemRepository;
  private final FoundItemRepository foundItemRepository;
  private final ItemMatchRepository itemMatchRepository;
  private final AnimalReportRepository animalReportRepository;
  private final AnimalReportMatchRepository animalReportMatchRepository;
  private final InquiryRepository inquiryRepository;
  private final OAuth2Service oauth2Service;

  @GetMapping("/login")
  public String loginPage(@RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "exception", required = false) String exception,
                          @RequestParam(value = "message", required = false) String message,
                          Model model) {
    model.addAttribute("error", error);
    model.addAttribute("exception", exception);
    model.addAttribute("message", message);
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
  public String joinProcess(@ModelAttribute com.example.dogo.dto.user.UserJoinDto userJoinDto) {

    // 프로필 이미지 저장
    String profileImageUrl = profileService.saveProfileImage(userJoinDto.getProfileImage());

    // 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(userJoinDto.getPassword());

    com.example.dogo.entity.user.User newUser = new com.example.dogo.entity.user.User(
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
    com.example.dogo.entity.user.User user = userDetails.getUser();
    
    com.example.dogo.entity.user.User dbUser = userRepository.findById(user.getUserNo()).orElseThrow();
    
    // 소셜 연동 해제 (카카오/네이버 등)
    oauth2Service.unlink(dbUser);

    // 분실물/습득물 매칭 결과 삭제 → 분실물/습득물 삭제
    itemMatchRepository.deleteByLostItemUser(dbUser);
    itemMatchRepository.deleteByFoundItemUser(dbUser);
    lostItemRepository.deleteByUser(dbUser);
    foundItemRepository.deleteByUser(dbUser);

    // 동물 신고 매칭 결과 삭제 → 동물 신고 삭제 (이미지/임베딩은 CASCADE)
    animalReportMatchRepository.deleteByMissingReportUser(dbUser);
    animalReportMatchRepository.deleteBySightingReportUser(dbUser);
    animalReportRepository.deleteByUser(dbUser);

    // 문의글 삭제
    inquiryRepository.deleteByUser(dbUser);

    // 소셜 계정 연결 정보 삭제
    userSocialAccountRepository.deleteByUser(dbUser);

    // 계정 정보 완전 삭제 (Hard Delete)
    userRepository.delete(dbUser);
    
    // 로그아웃 처리
    request.logout();
    
    return "redirect:/";
  }

  @PostMapping("/user/profile/update")
  @Transactional
  public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @ModelAttribute UserProfileUpdateDto updateDto) {
    com.example.dogo.entity.user.User user = userDetails.getUser();
    com.example.dogo.entity.user.User dbUser = userRepository.findById(user.getUserNo()).orElseThrow();

    // 닉네임 변경
    if (StringUtils.hasText(updateDto.getNickname())) {
      dbUser.setNickname(updateDto.getNickname());
    }

    // 비밀번호 변경
    if (StringUtils.hasText(updateDto.getPassword())) {
      if (updateDto.getPassword().equals(updateDto.getConfirmPassword())) {
        dbUser.setPassword(passwordEncoder.encode(updateDto.getPassword()));
      }
    }

    // 프로필 이미지 변경
    if (updateDto.getProfileImage() != null && !updateDto.getProfileImage().isEmpty()) {
      String profileImageUrl = profileService.saveProfileImage(updateDto.getProfileImage());
      dbUser.updateProfileImage(profileImageUrl);
    }

    userRepository.save(dbUser);

    // 세션 정보 갱신
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    CustomUserDetails newUserDetails = new CustomUserDetails(dbUser, userDetails.getAttributes());
    Authentication newAuth = new UsernamePasswordAuthenticationToken(newUserDetails, auth.getCredentials(), auth.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(newAuth);

    return "redirect:/userpage";
  }
}
