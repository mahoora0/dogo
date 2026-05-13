package com.example.dogo.entity;

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

	@Column(name = "MATCH_STATUS", nullable = false)
	private String matchStatus = "CANDIDATE";

	@Column(name = "REGDATE", nullable = false, updatable = false)
	private LocalDateTime regdate = LocalDateTime.now();

	@Column(name = "MODDATE", nullable = false)
	private LocalDateTime moddate = LocalDateTime.now();

	public ItemMatch(LostItem lostItem, FoundItem foundItem, BigDecimal matchScore) {
		this.lostItem = lostItem;
		this.foundItem = foundItem;
		this.matchScore = matchScore;
	}
}
