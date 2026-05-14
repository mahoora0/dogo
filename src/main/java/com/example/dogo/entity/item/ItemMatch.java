package com.example.dogo.entity.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "ITEM_MATCH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MATCH_ID")
	private Long matchId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LOST_ID", nullable = false)
	private LostItem lostItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FOUND_ID", nullable = false)
	private FoundItem foundItem;

	@Column(name = "MATCH_SCORE")
	private BigDecimal matchScore;

	@Column(name = "RULE_SCORE")
	private BigDecimal ruleScore;

	@Column(name = "SEMANTIC_SCORE")
	private BigDecimal semanticScore;

	@Column(name = "FINAL_SCORE")
	private BigDecimal finalScore;

	@Column(name = "MATCH_REASONS", columnDefinition = "TEXT")
	private String matchReasons;

	@Column(name = "MATCH_VERSION")
	private String matchVersion;

	@Column(name = "MODEL_NAME")
	private String modelName;

	@Column(name = "MATCH_STATUS", nullable = false)
	private String matchStatus = "CANDIDATE";

	@Column(name = "REGDATE", nullable = false, updatable = false)
	private LocalDateTime regdate = LocalDateTime.now();

	@Column(name = "MODDATE", nullable = false)
	private LocalDateTime moddate = LocalDateTime.now();

	public ItemMatch(LostItem lostItem, FoundItem foundItem, BigDecimal matchScore) {
		this(lostItem, foundItem, matchScore, null, matchScore, List.of(), "java-rule-v1", null);
	}

	public ItemMatch(
			LostItem lostItem,
			FoundItem foundItem,
			BigDecimal ruleScore,
			BigDecimal semanticScore,
			BigDecimal finalScore,
			List<String> matchReasons,
			String matchVersion,
			String modelName
	) {
		this.lostItem = lostItem;
		this.foundItem = foundItem;
		this.ruleScore = ruleScore;
		this.semanticScore = semanticScore;
		this.finalScore = finalScore;
		this.matchScore = finalScore;
		this.matchReasons = serializeReasons(matchReasons);
		this.matchVersion = matchVersion;
		this.modelName = modelName;
	}

	public List<String> getMatchReasonList() {
		if (matchReasons == null || matchReasons.isBlank()) {
			return List.of();
		}
		return Arrays.stream(matchReasons.split("\\R"))
				.map(String::trim)
				.filter(reason -> !reason.isEmpty())
				.toList();
	}

	public BigDecimal displayScore() {
		if (finalScore != null) {
			return finalScore;
		}
		return matchScore;
	}

	private String serializeReasons(List<String> reasons) {
		if (reasons == null || reasons.isEmpty()) {
			return null;
		}
		return String.join("\n", reasons.stream()
				.filter(reason -> reason != null && !reason.isBlank())
				.map(String::trim)
				.toList());
	}
}
