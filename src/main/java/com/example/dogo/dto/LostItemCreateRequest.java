package com.example.dogo.dto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public class LostItemCreateRequest {

	private String title;
	private String itemName;
	private String categoryMain;
	private String categorySub;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime lostAt;

	private String lostArea;
	private String lostPlace;
	private String contact;
	private String content;
	private MultipartFile image;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public String getCategoryMain() {
		return categoryMain;
	}

	public void setCategoryMain(String categoryMain) {
		this.categoryMain = categoryMain;
	}

	public String getCategorySub() {
		return categorySub;
	}

	public void setCategorySub(String categorySub) {
		this.categorySub = categorySub;
	}

	public LocalDateTime getLostAt() {
		return lostAt;
	}

	public void setLostAt(LocalDateTime lostAt) {
		this.lostAt = lostAt;
	}

	public String getLostArea() {
		return lostArea;
	}

	public void setLostArea(String lostArea) {
		this.lostArea = lostArea;
	}

	public String getLostPlace() {
		return lostPlace;
	}

	public void setLostPlace(String lostPlace) {
		this.lostPlace = lostPlace;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public MultipartFile getImage() {
		return image;
	}

	public void setImage(MultipartFile image) {
		this.image = image;
	}
}
