package com.example.dogo.entity.item;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

@Entity
@Table(name = "LOST_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LostItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "LOST_ID")
	private Long lostId;

	@OneToMany(mappedBy = "lostItem", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<LostItemImage> images = new ArrayList<>();

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

	@Column(name = "COLOR_NAME")
	private String colorName;

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

	@Column(name = "REGDATE", updatable = false)
	private LocalDateTime regDate;

	public LostItem(
			User user,
			String title,
			String content,
			String itemName,
			String categoryMain,
			String categorySub,
			String colorName,
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
		this.colorName = colorName;
		this.lostAt = lostAt;
		this.lostArea = lostArea;
		this.lostPlace = lostPlace;
		this.contact = contact;
	}

	public static LostItem fromPolice(
			String atcId,
			String title,
			String content,
			String itemName,
			String categoryMain,
			String categorySub,
			String colorName,
			LocalDateTime lostAt,
			String lostArea,
			String lostPlace,
			String contact
	) {
		LostItem lostItem = new LostItem();
		lostItem.sourceType = "POLICE";
		lostItem.atcId = atcId;
		lostItem.title = title;
		lostItem.content = content;
		lostItem.itemName = itemName;
		lostItem.categoryMain = categoryMain;
		lostItem.categorySub = categorySub;
		lostItem.colorName = colorName;
		lostItem.lostAt = lostAt;
		lostItem.lostArea = lostArea;
		lostItem.lostPlace = lostPlace;
		lostItem.contact = contact;
		lostItem.status = "WAITING";
		return lostItem;
	}

	public void updatePoliceDetail(
			String title,
			String content,
			String itemName,
			String categoryMain,
			String categorySub,
			String colorName,
			LocalDateTime lostAt,
			String lostArea,
			String lostPlace,
			String contact
	) {
		this.title = title;
		this.content = content;
		this.itemName = itemName;
		this.categoryMain = categoryMain;
		this.categorySub = categorySub;
		this.colorName = colorName;
		this.lostAt = lostAt;
		this.lostArea = lostArea;
		this.lostPlace = lostPlace;
		this.contact = contact;
	}

	@PrePersist
	void prePersist() {
		if (this.regDate == null) {
			this.regDate = LocalDateTime.now();
		}
	}
}
