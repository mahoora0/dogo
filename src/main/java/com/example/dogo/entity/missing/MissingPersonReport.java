package com.example.dogo.entity.missing;

import com.example.dogo.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MISSING_PERSON_REPORT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissingPersonReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "REPORT_ID")
	private Long reportId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_NO")
	private User user;

	@Column(name = "SOURCE_TYPE", nullable = false)
	private String sourceType = "USER";

	@Column(name = "EXTERNAL_ID")
	private String externalId;

	@Column(name = "API_PROVIDER")
	private String apiProvider;

	@Column(name = "RAW_PAYLOAD")
	private String rawPayload;

	@Column(name = "SYNCED_AT")
	private LocalDateTime syncedAt;

	@Column(name = "PERSON_NAME")
	private String personName;

	@Column(name = "GENDER")
	private String gender;

	@Column(name = "AGE", nullable = false)
	private Integer age;

	@Column(name = "NATIONALITY", nullable = false)
	private String nationality;

	@Column(name = "OCCURRED_AT", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "OCCURRED_PLACE", nullable = false)
	private String occurredPlace;

	@Column(name = "HEIGHT_CM")
	private Integer heightCm;

	@Column(name = "WEIGHT_KG")
	private BigDecimal weightKg;

	@Column(name = "BODY_TYPE", nullable = false)
	private String bodyType;

	@Column(name = "FACE_SHAPE", nullable = false)
	private String faceShape;

	@Column(name = "HAIR_COLOR", nullable = false)
	private String hairColor;

	@Column(name = "HAIR_STYLE", nullable = false)
	private String hairStyle;

	@Column(name = "CLOTHING", nullable = false)
	private String clothing;

	@Column(name = "SEARCH_CONTENT", length = 768)
	private String searchContent;

	@Column(name = "STATUS", nullable = false)
	private String status = "OPEN";

	@Column(name = "IS_DELETED", nullable = false)
	private boolean deleted;

	@Column(name = "REGDATE", nullable = false, updatable = false)
	private LocalDateTime regdate = LocalDateTime.now();

	@Column(name = "MODDATE", nullable = false)
	private LocalDateTime moddate = LocalDateTime.now();

	public MissingPersonReport(
			User user,
			Integer age,
			String nationality,
			LocalDateTime occurredAt,
			String occurredPlace,
			Integer heightCm,
			BigDecimal weightKg,
			String bodyType,
			String faceShape,
			String hairColor,
			String hairStyle,
			String clothing
	) {
		this.user = user;
		this.age = age;
		this.nationality = nationality;
		this.occurredAt = occurredAt;
		this.occurredPlace = occurredPlace;
		this.heightCm = heightCm;
		this.weightKg = weightKg;
		this.bodyType = bodyType;
		this.faceShape = faceShape;
		this.hairColor = hairColor;
		this.hairStyle = hairStyle;
		this.clothing = clothing;
	}

	public void setStatus(String status) {
		this.status = status;
		this.moddate = LocalDateTime.now();
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
		this.moddate = LocalDateTime.now();
	}

	public boolean cleanUpData() {
		boolean modified = false;
		if (this.heightCm != null && this.heightCm > 300) {
			this.heightCm = null;
			modified = true;
		}
		if (this.clothing != null && "null".equalsIgnoreCase(this.clothing.trim())) {
			this.clothing = "착의 정보 없음";
			modified = true;
		}
		if (this.bodyType != null && "null".equalsIgnoreCase(this.bodyType.trim())) {
			this.bodyType = "미상";
			modified = true;
		}
		if (this.faceShape != null && "null".equalsIgnoreCase(this.faceShape.trim())) {
			this.faceShape = "미상";
			modified = true;
		}
		if (this.hairColor != null && "null".equalsIgnoreCase(this.hairColor.trim())) {
			this.hairColor = "미상";
			modified = true;
		}
		if (this.hairStyle != null && "null".equalsIgnoreCase(this.hairStyle.trim())) {
			this.hairStyle = "미상";
			modified = true;
		}
		if (this.occurredPlace != null && "null".equalsIgnoreCase(this.occurredPlace.trim())) {
			this.occurredPlace = "장소 미상";
			modified = true;
		}

		if (this.rawPayload != null && (this.rawPayload.contains("<findChildList>") || (this.rawPayload.contains("<totalCount>") && this.rawPayload.contains("<item>")))) {
			String corrected = extractCorrectRecordXml(this.rawPayload, this.personName);
			if (corrected != null && !corrected.equals(this.rawPayload)) {
				this.rawPayload = corrected;
				modified = true;
			}
		}

		if (modified) {
			generateSearchContent();
			this.moddate = LocalDateTime.now();
		}
		return modified;
	}

	private String extractCorrectRecordXml(String rawPayload, String personName) {
		if (rawPayload == null) return null;
		if (personName == null || personName.isBlank()) return null;

		String[] tags = {"item", "list"};
		for (String tag : tags) {
			String startTag = "<" + tag + ">";
			String endTag = "</" + tag + ">";
			int index = 0;
			while (true) {
				int start = rawPayload.indexOf(startTag, index);
				if (start == -1) break;
				int end = rawPayload.indexOf(endTag, start);
				if (end == -1) break;

				String block = rawPayload.substring(start, end + endTag.length());
				if (block.contains("<nm>") && block.contains("</nm>")) {
					int nmStart = block.indexOf("<nm>");
					int nmEnd = block.indexOf("</nm>", nmStart);
					String nmContent = block.substring(nmStart + 4, nmEnd).trim();
					if (nmContent.startsWith("<![CDATA[")) {
						nmContent = nmContent.substring(9, nmContent.length() - 3).trim();
					}
					if (nmContent.equals(personName.trim())) {
						if (this.age != null && block.contains("<age>") && block.contains("</age>")) {
							int ageStart = block.indexOf("<age>");
							int ageEnd = block.indexOf("</age>", ageStart);
							String ageContent = block.substring(ageStart + 5, ageEnd).trim();
							try {
								int parsedAge = Integer.parseInt(ageContent);
								if (parsedAge != this.age) {
									index = end + endTag.length();
									continue;
								}
							} catch (Exception e) {
								// ignore
							}
						}
						return block;
					}
				}
				index = end + endTag.length();
			}
		}
		return null;
	}

	public static MissingPersonReport fromPublicApi(
			String apiProvider,
			String externalId,
			String rawPayload,
			Integer age,
			String nationality,
			LocalDateTime occurredAt,
			String occurredPlace,
			Integer heightCm,
			BigDecimal weightKg,
			String bodyType,
			String faceShape,
			String hairColor,
			String hairStyle,
			String clothing
	) {
		return fromPublicApi(
				apiProvider,
				externalId,
				rawPayload,
				null,
				null,
				age,
				nationality,
				occurredAt,
				occurredPlace,
				heightCm,
				weightKg,
				bodyType,
				faceShape,
				hairColor,
				hairStyle,
				clothing
		);
	}

	public static MissingPersonReport fromPublicApi(
			String apiProvider,
			String externalId,
			String rawPayload,
			String personName,
			String gender,
			Integer age,
			String nationality,
			LocalDateTime occurredAt,
			String occurredPlace,
			Integer heightCm,
			BigDecimal weightKg,
			String bodyType,
			String faceShape,
			String hairColor,
			String hairStyle,
			String clothing
	) {
		MissingPersonReport report = new MissingPersonReport();
		report.sourceType = "PUBLIC_API";
		report.apiProvider = apiProvider;
		report.externalId = externalId;
		report.rawPayload = rawPayload;
		report.syncedAt = LocalDateTime.now();
		report.personName = personName;
		report.gender = gender;
		report.age = age;
		report.nationality = nationality;
		report.occurredAt = occurredAt;
		report.occurredPlace = occurredPlace;
		report.heightCm = (heightCm != null && heightCm > 300) ? null : heightCm;
		report.weightKg = weightKg;
		report.bodyType = bodyType;
		report.faceShape = faceShape;
		report.hairColor = hairColor;
		report.hairStyle = hairStyle;
		report.clothing = clothing;
		return report;
	}

	public String getSourceLabel() {
		return "PUBLIC_API".equals(sourceType) ? "데이터 출처: 경찰청" : "사용자 제보";
	}

	@PrePersist
	@PreUpdate
	public void generateSearchContent() {
		String fullContent = String.join(" ",
				blankToEmpty(personName),
				blankToEmpty(gender),
				blankToEmpty(nationality),
				blankToEmpty(occurredPlace),
				blankToEmpty(bodyType),
				blankToEmpty(faceShape),
				blankToEmpty(hairColor),
				blankToEmpty(hairStyle),
				blankToEmpty(clothing)
		).toLowerCase().replaceAll("\\s+", " ").trim();

		if (fullContent.length() > 768) {
			this.searchContent = fullContent.substring(0, 768);
		} else {
			this.searchContent = fullContent;
		}
	}

	private String blankToEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}
