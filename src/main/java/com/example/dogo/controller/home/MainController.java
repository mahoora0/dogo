package com.example.dogo.controller.home;

import com.example.dogo.dto.item.RecentItemView;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.item.FoundItemService;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.item.RegistrationOptionService;
import com.example.dogo.service.match.ItemMatchService;
import com.example.dogo.service.missing.MissingPersonService;
import com.example.dogo.service.animal.AnimalReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

  private static final int RECENT_ITEM_LIMIT = 8;

  private final LostItemService lostItemService;
  private final FoundItemService foundItemService;
  private final ItemMatchService itemMatchService;
  private final MissingPersonService missingPersonService;
  private final AnimalReportService animalReportService;
  private final RegistrationOptionService registrationOptionService;

  @GetMapping("/")
  public String index(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
    List<RecentItemView> recentItems = new ArrayList<>();
    recentItems.addAll(lostItemService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.addAll(foundItemService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.addAll(missingPersonService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.addAll(animalReportService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.sort((a, b) -> {
      java.time.LocalDateTime dateA = ("PERSON".equals(a.type()) || "ANIMAL".equals(a.type())) ? a.itemAt() : a.regDate();
      java.time.LocalDateTime dateB = ("PERSON".equals(b.type()) || "ANIMAL".equals(b.type())) ? b.itemAt() : b.regDate();
      if (dateA == null && dateB == null) return 0;
      if (dateA == null) return 1;
      if (dateB == null) return -1;
      int compareDate = dateB.compareTo(dateA);
      if (compareDate != 0) {
        return compareDate;
      }
      return b.id().compareTo(a.id());
    });

    // 회원이 최소 1개 이상 분실물을 등록했을 때만 매칭 알림 조회
    if (userDetails != null) {
      boolean hasLostItems = lostItemService.hasLostItems(userDetails.getUser());
      model.addAttribute("hasLostItems", hasLostItems);

      if (hasLostItems) {
        // 종 아이콘 배지에 표시할 읽지 않은 매칭 알림 수 조회 및 모델 추가
        long unreadCount = itemMatchService.getUnreadMatchCount(userDetails.getUser());
        model.addAttribute("unreadMatchCount", unreadCount);
      } else {
        model.addAttribute("unreadMatchCount", 0L);
      }
    }

    model.addAttribute("currentUri", "/");
    model.addAttribute("searchCategories", lostItemService.getSearchCategoryNames());
    model.addAttribute("recentItems", recentItems);
    return "index";
  }

  @GetMapping("/privacy")
  public String privacyPage() {
    return "user/privacy";
  }
}
