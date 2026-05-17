package com.example.dogo.controller.item;

import com.example.dogo.dto.match.MatchCandidateView;
import com.example.dogo.service.match.ItemMatchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchApiController {

	private static final Logger log = LoggerFactory.getLogger(MatchApiController.class);

	private final ItemMatchService itemMatchService;

	@PostMapping("/lost-items/{id}/rematch")
	public ResponseEntity<List<MatchCandidateView>> rematchLost(@PathVariable Long id) {
		log.info("분실물 수동 재매칭 요청: lostId={}", id);
		itemMatchService.matchForLostItemId(id);
		return ResponseEntity.ok(itemMatchService.getMatchesForLostItem(id));
	}

	@PostMapping("/found-items/{id}/rematch")
	public ResponseEntity<List<MatchCandidateView>> rematchFound(@PathVariable Long id) {
		log.info("습득물 수동 재매칭 요청: foundId={}", id);
		itemMatchService.matchForFoundItemId(id);
		return ResponseEntity.ok(itemMatchService.getMatchesForFoundItem(id));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
	}
}
