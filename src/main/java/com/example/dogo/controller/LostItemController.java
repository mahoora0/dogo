package com.example.dogo.controller;

import com.example.dogo.dto.LostItemView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class LostItemController {

	private final List<LostItemView> lostItems = List.of(
			new LostItemView(
					1L,
					"검정 카드지갑",
					"지갑",
					"서울 강남구",
					"강남역 11번 출구 근처",
					LocalDate.of(2026, 5, 2),
					"대기중",
					"신분증과 체크카드가 들어있는 검정색 카드지갑입니다.",
					"https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=900&q=80"
			),
			new LostItemView(
					2L,
					"흰색 무선 이어폰",
					"전자기기",
					"서울 마포구",
					"홍대입구역 2호선 승강장",
					LocalDate.of(2026, 5, 4),
					"매칭중",
					"케이스 오른쪽 아래에 작은 스크래치가 있습니다.",
					"https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?auto=format&fit=crop&w=900&q=80"
			),
			new LostItemView(
					3L,
					"남색 백팩",
					"가방",
					"경기 성남시",
					"판교역 버스정류장",
					LocalDate.of(2026, 5, 5),
					"회수완료",
					"노트북 파우치와 회색 우산이 들어있습니다.",
					"https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=80"
			)
	);

	@GetMapping("/")
	public String home() {
		return "redirect:/lost-items";
	}

	@GetMapping("/lost-items")
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String area,
			@RequestParam(required = false) String status,
			Model model
	) {
		List<LostItemView> filteredItems = lostItems.stream()
				.filter(item -> contains(item.name(), keyword) || contains(item.description(), keyword))
				.filter(item -> matches(item.category(), category))
				.filter(item -> contains(item.area(), area))
				.filter(item -> matches(item.status(), status))
				.toList();

		model.addAttribute("lostItems", filteredItems);
		model.addAttribute("keyword", keyword);
		model.addAttribute("category", category);
		model.addAttribute("area", area);
		model.addAttribute("status", status);
		return "lost-items/list";
	}

	@GetMapping("/lost-items/new")
	public String createForm() {
		return "lost-items/new";
	}

	@PostMapping("/lost-items")
	public String create() {
		return "redirect:/lost-items";
	}

	@GetMapping("/lost-items/{id}")
	public String detail(@PathVariable Long id, Model model) {
		LostItemView lostItem = lostItems.stream()
				.filter(item -> item.id().equals(id))
				.findFirst()
				.orElse(lostItems.get(0));

		model.addAttribute("lostItem", lostItem);
		return "lost-items/detail";
	}

	private boolean contains(String source, String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return true;
		}
		return source != null && source.toLowerCase().contains(keyword.toLowerCase());
	}

	private boolean matches(String source, String value) {
		if (!StringUtils.hasText(value)) {
			return true;
		}
		return source != null && source.equals(value);
	}
}
