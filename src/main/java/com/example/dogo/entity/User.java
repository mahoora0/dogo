package com.example.dogo.entity;

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
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	public User(String email, String nickname, String phone) {
		this.email = email;
		this.nickname = nickname;
		this.phone = phone;
	}
}
