package com.example.dogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "USERS")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "USER_NO")
	private Long userNo;

	@Column(name = "EMAIL")
	private String email;

	@Column(name = "NICKNAME")
	private String nickname;

	@Column(name = "PHONE")
	private String phone;

	@Column(name = "ROLE", nullable = false)
	private String role = "USER";

	@Column(name = "STATUS", nullable = false)
	private String status = "ACTIVE";

	protected User() {
	}

	public User(String email, String nickname, String phone) {
		this.email = email;
		this.nickname = nickname;
		this.phone = phone;
	}

	public Long getUserNo() {
		return userNo;
	}

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}

	public String getPhone() {
		return phone;
	}
}
