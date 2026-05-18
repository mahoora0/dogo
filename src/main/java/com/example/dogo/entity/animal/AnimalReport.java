package com.example.dogo.entity.animal;

import com.example.dogo.entity.area.Area;
import com.example.dogo.entity.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ANIMAL_REPORT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "REPORT_ID")
	private Long reportId;

	@OneToMany(mappedBy = "animalReport", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<AnimalReportImage> images = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_NO", nullable = false)
	private User user;

	@Column(name = "REPORT_TYPE", nullable = false)
	private String reportType;

	@Column(name = "STATUS", nullable = false)
	private String status = "OPEN";

	@Column(name = "TITLE")
	private String title;

	@Column(name = "EVENT_DATE", nullable = false)
	private LocalDate eventDate;

	@Column(name = "EVENT_TIME")
	private LocalTime eventTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REGION_AREA_ID")
	private Area regionArea;

	@Column(name = "REGION_NAME", nullable = false)
	private String regionName;

	@Column(name = "DETAIL_PLACE", nullable = false)
	private String detailPlace;

	@Column(name = "CONTACT_PHONE")
	private String contactPhone;

	@Column(name = "CONTACT_PUBLIC", nullable = false)
	private boolean contactPublic = true;

	@Column(name = "SIGHTING_CARE_STATUS")
	private String sightingCareStatus;

	@Column(name = "ANIMAL_TYPE", nullable = false)
	private String animalType;

	@Column(name = "BREED_NAME")
	private String breedName;

	@Column(name = "GENDER", nullable = false)
	private String gender = "UNKNOWN";

	@Column(name = "NEUTERED_STATUS", nullable = false)
	private String neuteredStatus = "UNKNOWN";

	@Column(name = "AGE_VALUE")
	private Integer ageValue;

	@Column(name = "AGE_UNIT")
	private String ageUnit;

	@Column(name = "WEIGHT_KG")
	private BigDecimal weightKg;

	@Column(name = "FUR_COLOR")
	private String furColor;

	@Column(name = "DISTINCTIVE_MARKS")
	private String distinctiveMarks;

	@Column(name = "CONTENT")
	private String content;

	@Column(name = "VIEW_COUNT", nullable = false)
	private int viewCount;

	@Column(name = "IS_DELETED", nullable = false)
	private boolean deleted;

	@Column(name = "REGDATE", nullable = false, updatable = false)
	private LocalDateTime regdate = LocalDateTime.now();

	@Column(name = "MODDATE", nullable = false)
	private LocalDateTime moddate = LocalDateTime.now();

	public AnimalReport(
			User user,
			String reportType,
			String title,
			LocalDate eventDate,
			LocalTime eventTime,
			Area regionArea,
			String regionName,
			String detailPlace,
			String contactPhone,
			boolean contactPublic,
			String sightingCareStatus,
			String animalType,
			String breedName,
			String gender,
			String neuteredStatus,
			Integer ageValue,
			String ageUnit,
			BigDecimal weightKg,
			String furColor,
			String distinctiveMarks,
			String content
	) {
		this.user = user;
		this.reportType = reportType;
		this.title = title;
		this.eventDate = eventDate;
		this.eventTime = eventTime;
		this.regionArea = regionArea;
		this.regionName = regionName;
		this.detailPlace = detailPlace;
		this.contactPhone = contactPhone;
		this.contactPublic = contactPublic;
		this.sightingCareStatus = sightingCareStatus;
		this.animalType = animalType;
		this.breedName = breedName;
		this.gender = gender;
		this.neuteredStatus = neuteredStatus;
		this.ageValue = ageValue;
		this.ageUnit = ageUnit;
		this.weightKg = weightKg;
		this.furColor = furColor;
		this.distinctiveMarks = distinctiveMarks;
		this.content = content;
	}

	public void update(
			String reportType,
			String title,
			LocalDate eventDate,
			LocalTime eventTime,
			Area regionArea,
			String regionName,
			String detailPlace,
			String contactPhone,
			boolean contactPublic,
			String sightingCareStatus,
			String animalType,
			String breedName,
			String gender,
			String neuteredStatus,
			Integer ageValue,
			String ageUnit,
			java.math.BigDecimal weightKg,
			String furColor,
			String distinctiveMarks,
			String content
	) {
		this.reportType = reportType;
		this.title = title;
		this.eventDate = eventDate;
		this.eventTime = eventTime;
		this.regionArea = regionArea;
		this.regionName = regionName;
		this.detailPlace = detailPlace;
		this.contactPhone = contactPhone;
		this.contactPublic = contactPublic;
		this.sightingCareStatus = sightingCareStatus;
		this.animalType = animalType;
		this.breedName = breedName;
		this.gender = gender;
		this.neuteredStatus = neuteredStatus;
		this.ageValue = ageValue;
		this.ageUnit = ageUnit;
		this.weightKg = weightKg;
		this.furColor = furColor;
		this.distinctiveMarks = distinctiveMarks;
		this.content = content;
		this.moddate = LocalDateTime.now();
	}

	public void increaseViewCount() {
		this.viewCount++;
		this.moddate = LocalDateTime.now();
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
