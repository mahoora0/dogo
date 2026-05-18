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

    // [로그인 시 실시간 매칭 작동] 회원이 최소 1개 이상 분실물을 등록했을 때만 매칭 가동 및 알림 갱신
    if (userDetails != null) {
      boolean hasLostItems = lostItemService.hasLostItems(userDetails.getUser());
      model.addAttribute("hasLostItems", hasLostItems);
      
      if (hasLostItems) {
        // 1. 로그인한 회원의 미완료 분실물들에 대해 최신 습득물 매칭 알고리즘 백그라운드 구동
        itemMatchService.matchForUserLostItems(userDetails.getUser());
        // 2. 종 아이콘 배지에 표시할 읽지 않은 매칭 알림 수 조회 및 모델 추가
        long unreadCount = itemMatchService.getUnreadMatchCount(userDetails.getUser());
        model.addAttribute("unreadMatchCount", unreadCount);
      } else {
        model.addAttribute("unreadMatchCount", 0L);
      }
    }

    model.addAttribute("currentUri", "/");
    model.addAttribute("searchCategories", lostItemService.getSearchCategoryNames());
    return "index";
  }
}
