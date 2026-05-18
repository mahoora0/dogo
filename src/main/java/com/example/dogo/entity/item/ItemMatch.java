package com.example.dogo.entity.item;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

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
		String trimmedReasons = matchReasons.trim();
		if (trimmedReasons.startsWith("[")) {
			try {
				return OBJECT_MAPPER.readValue(trimmedReasons, STRING_LIST_TYPE).stream()
						.filter(reason -> reason != null && !reason.isBlank())
						.map(String::trim)
						.toList();
			} catch (JsonProcessingException ignored) {
				// Fall through for legacy newline-separated rows.
			}
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

	public void markAsRead() {
		if ("CANDIDATE".equals(this.matchStatus)) {
			this.matchStatus = "READ";
			this.moddate = LocalDateTime.now();
		}
	}

	private String serializeReasons(List<String> reasons) {
		if (reasons == null || reasons.isEmpty()) {
			return null;
		}
		List<String> filteredReasons = reasons.stream()
				.filter(reason -> reason != null && !reason.isBlank())
				.map(String::trim)
				.toList();
		if (filteredReasons.isEmpty()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(filteredReasons);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("매칭 사유를 JSON으로 변환할 수 없습니다.", exception);
		}
	}
}
