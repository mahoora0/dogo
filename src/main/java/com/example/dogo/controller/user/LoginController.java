package com.example.dogo.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.example.dogo.repository.item.LostItemImageRepository;
import com.example.dogo.repository.item.FoundItemImageRepository;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.animal.AnimalReportMatchRepository;
import com.example.dogo.repository.Support.InquiryRepository;
import com.example.dogo.repository.ChatMessageRepository;
import com.example.dogo.repository.ChatRoomRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.dto.item.RecentItemView;
import org.springframework.ui.Model;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@Controller
@RequiredArgsConstructor
public class LoginController {
    
  private final UserRepository userRepository;
  private final UserSocialAccountRepository userSocialAccountRepository;
  private final ProfileService profileService;
  private final PasswordEncoder passwordEncoder;
  private final LostItemRepository lostItemRepository;
  private final FoundItemRepository foundItemRepository;
  private final LostItemImageRepository lostItemImageRepository;
  private final FoundItemImageRepository foundItemImageRepository;
  private final ItemMatchRepository itemMatchRepository;
  private final AnimalReportRepository animalReportRepository;
  private final AnimalReportMatchRepository animalReportMatchRepository;
  private final InquiryRepository inquiryRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final OAuth2Service oauth2Service;
  private final MissingPersonRepository missingPersonRepository;

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

  @GetMapping("/api/user/{userNo}")
  @ResponseBody
  public ResponseEntity<?> getUserProfile(@PathVariable("userNo") Long userNo) {
    return userRepository.findById(userNo)
        .map(user -> {
            int lostCount = lostItemRepository.findByUserAndDeletedFalseOrderByRegDateDesc(user).size();
            int foundCount = foundItemRepository.findByUserAndDeletedFalseOrderByRegDateDesc(user).size();
            
            return ResponseEntity.ok(java.util.Map.of(
                "nickname", user.getNickname(),
                "profileImageUrl", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "/images/logoNoName.png",
                "email", user.getEmail() != null ? user.getEmail() : "이메일 없음",
                "phone", user.getPhone() != null ? user.getPhone() : "연락처 없음",
                "regDate", user.getRegDate() != null ? user.getRegDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")) : "가입일 없음",
                "lostCount", lostCount,
                "foundCount", foundCount
            ));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/api/chat/item/{itemType}/{itemId}")
  @ResponseBody
  public ResponseEntity<?> getItemSimpleDetail(@PathVariable("itemType") String itemType, @PathVariable("itemId") Long itemId) {
    if ("FOUND".equals(itemType)) {
      return foundItemRepository.findById(itemId)
          .map(item -> {
              List<com.example.dogo.entity.item.FoundItemImage> imgs = foundItemImageRepository.findByFoundItemOrderBySortOrderAscImageIdAsc(item);
              String imgUrl = !imgs.isEmpty() ? imgs.get(0).getImageUrl() : "/images/noImageSize.png";
              String formattedDate = item.getFoundAt() != null ? item.getFoundAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")) : "날짜 정보 없음";
              
              return ResponseEntity.ok(java.util.Map.of(
                  "title", item.getTitle() != null ? item.getTitle() : "제목 없음",
                  "itemName", item.getItemName() != null ? item.getItemName() : "물품명 없음",
                  "category", (item.getCategoryMain() != null ? item.getCategoryMain() : "") + (item.getCategorySub() != null ? " > " + item.getCategorySub() : ""),
                  "color", item.getColorName() != null ? item.getColorName() : "색상 정보 없음",
                  "place", item.getFoundPlace() != null ? item.getFoundPlace() : "장소 정보 없음",
                  "date", formattedDate,
                  "status", getStatusKorean(item.getStatus()),
                  "content", item.getContent() != null ? item.getContent() : "상세내용 없음",
                  "imageUrl", imgUrl
              ));
          })
          .orElse(ResponseEntity.notFound().build());
    } else if ("LOST".equals(itemType)) {
      return lostItemRepository.findById(itemId)
          .map(item -> {
              List<com.example.dogo.entity.item.LostItemImage> imgs = lostItemImageRepository.findByLostItemOrderBySortOrderAscImageIdAsc(item);
              String imgUrl = !imgs.isEmpty() ? imgs.get(0).getImageUrl() : "/images/noImageSize.png";
              String formattedDate = item.getLostAt() != null ? item.getLostAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")) : "날짜 정보 없음";
              
              return ResponseEntity.ok(java.util.Map.of(
                  "title", item.getTitle() != null ? item.getTitle() : "제목 없음",
                  "itemName", item.getItemName() != null ? item.getItemName() : "물품명 없음",
                  "category", (item.getCategoryMain() != null ? item.getCategoryMain() : "") + (item.getCategorySub() != null ? " > " + item.getCategorySub() : ""),
                  "color", item.getColorName() != null ? item.getColorName() : "색상 정보 없음",
                  "place", item.getLostPlace() != null ? item.getLostPlace() : "장소 정보 없음",
                  "date", formattedDate,
                  "status", getStatusKorean(item.getStatus()),
                  "content", item.getContent() != null ? item.getContent() : "상세내용 없음",
                  "imageUrl", imgUrl
              ));
          })
          .orElse(ResponseEntity.notFound().build());
    }
    return ResponseEntity.badRequest().build();
  }

  private String getStatusKorean(String status) {
    if (status == null) return "알수없음";
    switch (status.toUpperCase()) {
      case "KEEPING": return "보관중";
      case "MATCHING": return "매칭중";
      case "COMPLETED": return "완료";
      case "WAITING": return "대기중";
      default: return status;
    }
  }

  @GetMapping("/userpage")
  @Transactional(readOnly = true)
  public String userPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails == null) {
      return "redirect:/login";
    }
    com.example.dogo.entity.user.User user = userDetails.getUser();

    List<com.example.dogo.entity.item.LostItem> lostItems = lostItemRepository.findByUserAndDeletedFalseOrderByRegDateDesc(user);
    List<com.example.dogo.entity.item.FoundItem> foundItems = foundItemRepository.findByUserAndDeletedFalseOrderByRegDateDesc(user);
    List<com.example.dogo.entity.Support.Inquiry> inquiries = inquiryRepository.findByUserOrderByRegdateDescInquiryIdDesc(user);
    List<AnimalReport> animalReports = animalReportRepository.findByUserAndDeletedFalseOrderByRegdateDesc(user);
    List<MissingPersonReport> personReports = missingPersonRepository.findByUserAndDeletedFalseOrderByRegdateDesc(user);

    List<RecentItemView> userActivities = new ArrayList<>();

    for (com.example.dogo.entity.item.LostItem item : lostItems) {
      String imageUrl = lostItemImageRepository.findFirstByLostItemOrderBySortOrderAscImageIdAsc(item)
          .map(com.example.dogo.entity.item.LostItemImage::getImageUrl)
          .orElse("/images/noImageSize.png");

      userActivities.add(new RecentItemView(
          item.getLostId(),
          "LOST",
          "분실물",
          item.getTitle(),
          item.getCategoryMain(),
          item.getLostPlace(),
          item.getRegDate() != null ? item.getRegDate() : item.getLostAt(),
          item.getStatus(),
          lostStatusLabel(item.getStatus()),
          imageUrl
      ));
    }

    for (com.example.dogo.entity.item.FoundItem item : foundItems) {
      String imageUrl = foundItemImageRepository.findFirstByFoundItemOrderBySortOrderAscImageIdAsc(item)
          .map(com.example.dogo.entity.item.FoundItemImage::getImageUrl)
          .orElse("/images/noImageSize.png");

      userActivities.add(new RecentItemView(
          item.getFoundId(),
          "FOUND",
          "습득물",
          item.getTitle(),
          item.getCategoryMain(),
          item.getFoundPlace() != null ? item.getFoundPlace() : item.getFoundArea(),
          item.getRegDate() != null ? item.getRegDate() : item.getFoundAt(),
          item.getStatus(),
          foundStatusLabel(item.getStatus()),
          imageUrl
      ));
    }

    for (com.example.dogo.entity.Support.Inquiry inquiry : inquiries) {
      String statusLabel = switch (inquiry.getStatus()) {
        case "ANSWERED" -> "답변완료";
        case "CHECKING" -> "검토중";
        default -> "답변대기";
      };

      userActivities.add(new RecentItemView(
          inquiry.getInquiryId(),
          "INQUIRY",
          "1:1 문의",
          inquiry.getTitle(),
          inquiry.getCategory(),
          "고객센터",
          inquiry.getRegdate() != null ? inquiry.getRegdate() : java.time.LocalDateTime.now(),
          inquiry.getStatus(),
          statusLabel,
          "/images/noImageSize.png"
      ));
    }

    for (AnimalReport report : animalReports) {
      String imageUrl = "/images/noImageSize.png";
      if (report.getImages() != null && !report.getImages().isEmpty()) {
        imageUrl = report.getImages().get(0).getImageUrl();
      }

      String typeLabel = "MISSING".equals(report.getReportType()) ? "실종동물 신고" : "제보 및 목격";
      String statusLabel = "OPEN".equals(report.getStatus()) ? "접수" : ("MATCHING".equals(report.getStatus()) ? "매칭중" : "해결완료");

      userActivities.add(new RecentItemView(
          report.getReportId(),
          "ANIMAL_REPORT",
          typeLabel,
          report.getTitle(),
          report.getAnimalType() + " (" + report.getBreedName() + ")",
          report.getRegionName() + " " + report.getDetailPlace(),
          report.getRegdate() != null ? report.getRegdate() : java.time.LocalDateTime.now(),
          report.getStatus(),
          statusLabel,
          imageUrl
      ));
    }

    for (MissingPersonReport report : personReports) {
      String statusLabel = "OPEN".equals(report.getStatus()) ? "접수" : "해결완료";

      userActivities.add(new RecentItemView(
          report.getReportId(),
          "MISSING_PERSON",
          "실종자 신고",
          report.getAge() + "세 " + report.getNationality() + " 실종",
          report.getBodyType() + " (" + report.getHairStyle() + ")",
          report.getOccurredPlace(),
          report.getRegdate() != null ? report.getRegdate() : java.time.LocalDateTime.now(),
          report.getStatus(),
          statusLabel,
          "/images/noImageSize.png"
      ));
    }

    userActivities.sort(Comparator.comparing(RecentItemView::itemAt).reversed());

    model.addAttribute("userActivities", userActivities);
    return "user/userpage";
  }

  private String lostStatusLabel(String status) {
    if (status == null) return "대기중";
    return switch (status) {
      case "MATCHING" -> "매칭중";
      case "FOUND" -> "회수완료";
      default -> "대기중";
    };
  }

  private String foundStatusLabel(String status) {
    if (status == null) return "보관중";
    return switch (status) {
      case "MATCHING" -> "매칭중";
      case "RETURNED" -> "수령완료";
      default -> "보관중";
    };
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
    
    // 즉시 하드 삭제하지 않고, 상태를 'WITHDRAWN'으로 전환하여 7일간 보존함 (7일 후 스케줄러가 하드 삭제)
    dbUser.withdraw();
    userRepository.save(dbUser);
    
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
