package com.example.dogo.controller.home;

import com.example.dogo.dto.item.RecentItemView;
import com.example.dogo.security.CustomUserDetails;
import com.example.dogo.service.item.FoundItemService;
import com.example.dogo.service.item.LostItemService;
import com.example.dogo.service.match.ItemMatchService;
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

  private static final int RECENT_ITEM_LIMIT = 6;

  private final LostItemService lostItemService;
  private final FoundItemService foundItemService;
  private final ItemMatchService itemMatchService;

  @GetMapping("/")
  public String index(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
    List<RecentItemView> recentItems = new ArrayList<>();
    recentItems.addAll(lostItemService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.addAll(foundItemService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.sort(Comparator.comparing(RecentItemView::itemAt).reversed());

    // 로그인한 사용자의 안 읽은 매칭 알림 건수 조회
    if (userDetails != null) {
      long unreadCount = itemMatchService.getUnreadMatchCount(userDetails.getUser());
      model.addAttribute("unreadMatchCount", unreadCount);
    }

    model.addAttribute("currentUri", "/");
    model.addAttribute("searchCategories", lostItemService.getSearchCategoryNames());
    return "index";
  }
}
