package com.example.dogo.entity.animal;

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
@Table(name = "ANIMAL_REPORT_MATCH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalReportMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MATCH_ID")
	private Long matchId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MISSING_REPORT_ID", nullable = false)
	private AnimalReport missingReport;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SIGHTING_REPORT_ID", nullable = false)
	private AnimalReport sightingReport;

	@Column(name = "MATCH_SCORE")
	private BigDecimal matchScore;

	@Column(name = "RULE_SCORE")
	private BigDecimal ruleScore;

	@Column(name = "SEMANTIC_SCORE")
	private BigDecimal semanticScore;

	@Column(name = "FINAL_SCORE")
	private BigDecimal finalScore;

	@Column(name = "MATCH_REASONS")
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

	public AnimalReportMatch(
			AnimalReport missingReport,
			AnimalReport sightingReport,
			BigDecimal imageScore,
			String modelName
	) {
		this.missingReport = missingReport;
		this.sightingReport = sightingReport;
		this.semanticScore = imageScore;
		this.finalScore = imageScore;
		this.matchVersion = "clip-image-v1";
		this.modelName = modelName;
	}
}
