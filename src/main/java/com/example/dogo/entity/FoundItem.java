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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "FOUND_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FoundItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "FOUND_ID")
	private Long foundId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_NO")
	private User user;

	@Column(name = "SOURCE_TYPE", nullable = false)
	private String sourceType = "USER";

	@Column(name = "ATC_ID")
	private String atcId;

	@Column(name = "FD_SN")
	private Integer fdSn;

	@Column(name = "TITLE")
	private String title;

	@Column(name = "CONTENT")
	private String content;

	@Column(name = "ITEM_NAME", nullable = false)
	private String itemName;

	@Column(name = "CATEGORY_MAIN")
	private String categoryMain;

	@Column(name = "CATEGORY_SUB")
	private String categorySub;

	@Column(name = "COLOR_NAME")
	private String colorName;

	@Column(name = "FOUND_AT", nullable = false)
	private LocalDateTime foundAt;

	@Column(name = "FOUND_AREA")
	private String foundArea;

	@Column(name = "FOUND_PLACE")
	private String foundPlace;

	@Column(name = "KEEP_PLACE")
	private String keepPlace;

	@Column(name = "CUSTODY_STATUS")
	private String custodyStatus;

	@Column(name = "RECEIVE_TYPE")
	private String receiveType;

	@Column(name = "MANAGE_NO")
	private String manageNo;

	@Column(name = "SERIAL_NO")
	private String serialNo;

	@Column(name = "MODEL_CODE")
	private String modelCode;

	@Column(name = "IMEI")
	private String imei;

	@Column(name = "OWNER_NAME")
	private String ownerName;

	@Column(name = "STATUS", nullable = false)
	private String status = "KEEPING";

	@Column(name = "IS_DELETED", nullable = false)
	private boolean deleted;

	public FoundItem(
			User user,
			String title,
			String itemName,
			String categoryMain,
			String categorySub,
			LocalDateTime foundAt,
			String foundArea,
			String foundPlace,
			String keepPlace,
			String colorName,
			String content
	) {
		this.user = user;
		this.title = title;
		this.content = content;
		this.itemName = itemName;
		this.categoryMain = categoryMain;
		this.categorySub = categorySub;
		this.foundAt = foundAt;
		this.foundArea = foundArea;
		this.foundPlace = foundPlace;
		this.keepPlace = keepPlace;
		this.colorName = colorName;
	}
}
