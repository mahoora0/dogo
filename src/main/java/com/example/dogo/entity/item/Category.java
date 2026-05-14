package com.example.dogo.entity.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CATEGORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CATEGORY_ID")
	private Long categoryId;

	@Column(name = "CATEGORY_NAME", nullable = false)
	private String categoryName;

	@Column(name = "CATEGORY_CODE")
	private String categoryCode;

	@Column(name = "PARENT_CATEGORY_ID")
	private Long parentCategoryId;

	@Column(name = "LEVEL", nullable = false)
	private int categoryLevel;

	@Column(name = "SORT_ORDER", nullable = false)
	private int sortOrder;

	@Column(name = "IS_ACTIVE", nullable = false)
	private boolean active;
}
