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

import java.time.LocalDateTime;

@Entity
@Table(name = "LOST_ITEM")
public class LostItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "LOST_ID")
	private Long lostId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_NO")
	private User user;

	@Column(name = "SOURCE_TYPE", nullable = false)
	private String sourceType = "USER";

	@Column(name = "ATC_ID")
	private String atcId;

	@Column(name = "TITLE", nullable = false)
	private String title;

	@Column(name = "CONTENT")
	private String content;

	@Column(name = "ITEM_NAME", nullable = false)
	private String itemName;

	@Column(name = "CATEGORY_MAIN")
	private String categoryMain;

	@Column(name = "CATEGORY_SUB")
	private String categorySub;

	@Column(name = "LOST_AT", nullable = false)
	private LocalDateTime lostAt;

	@Column(name = "LOST_AREA")
	private String lostArea;

	@Column(name = "LOST_PLACE", nullable = false)
	private String lostPlace;

	@Column(name = "CONTACT")
	private String contact;

	@Column(name = "STATUS", nullable = false)
	private String status = "WAITING";

	@Column(name = "IS_DELETED", nullable = false)
	private boolean deleted;

	protected LostItem() {
	}

	public LostItem(
			User user,
			String title,
			String content,
			String itemName,
			String categoryMain,
			String categorySub,
			LocalDateTime lostAt,
			String lostArea,
			String lostPlace,
			String contact
	) {
		this.user = user;
		this.title = title;
		this.content = content;
		this.itemName = itemName;
		this.categoryMain = categoryMain;
		this.categorySub = categorySub;
		this.lostAt = lostAt;
		this.lostArea = lostArea;
		this.lostPlace = lostPlace;
		this.contact = contact;
	}

	public Long getLostId() {
		return lostId;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public String getItemName() {
		return itemName;
	}

	public String getCategoryMain() {
		return categoryMain;
	}

	public String getCategorySub() {
		return categorySub;
	}

	public LocalDateTime getLostAt() {
		return lostAt;
	}

	public String getLostArea() {
		return lostArea;
	}

	public String getLostPlace() {
		return lostPlace;
	}

	public String getContact() {
		return contact;
	}

	public String getStatus() {
		return status;
	}

	public boolean isDeleted() {
		return deleted;
	}
}
