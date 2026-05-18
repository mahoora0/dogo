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
}
