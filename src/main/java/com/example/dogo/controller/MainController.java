package com.example.dogo.controller;

import com.example.dogo.dto.RecentItemView;
import com.example.dogo.service.FoundItemService;
import com.example.dogo.service.LostItemService;
import lombok.RequiredArgsConstructor;
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

  @GetMapping("/")
  public String index(Model model) {
    List<RecentItemView> recentItems = new ArrayList<>();
    recentItems.addAll(lostItemService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.addAll(foundItemService.getRecentItems(RECENT_ITEM_LIMIT));
    recentItems.sort(Comparator.comparing(RecentItemView::itemAt).reversed());

    model.addAttribute("currentUri", "/");
    model.addAttribute("searchCategories", lostItemService.getSearchCategoryNames());
    model.addAttribute("recentItems", recentItems.subList(0, Math.min(RECENT_ITEM_LIMIT, recentItems.size())));
    return "index";
  }
}
