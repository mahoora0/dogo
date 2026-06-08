package com.example.dogo.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.PrePersist;

@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "USER_NO")
	private Long userNo;

	@Column(name = "LOGIN_ID")
	private String loginId;

	@Column(name = "PASSWORD")
	private String password;

	@Column(name = "EMAIL")
	private String email;

	@Column(name = "NICKNAME")
	private String nickname;

	@Column(name = "PHONE")
	private String phone;

	@Column(name = "PROFILE_IMAGE_URL")
	private String profileImageUrl;

	@Column(name = "ROLE", nullable = false)
	private String role = "USER";

	@Column(name = "STATUS", nullable = false)
	private String status = "ACTIVE";

	@Column(name = "REGDATE", nullable = false, updatable = false)
	private java.time.LocalDateTime regDate;

	@Column(name = "WITHDRAWN_AT")
	private java.time.LocalDateTime withdrawnAt;

	@Column(name = "REPORT_COUNT_ADJUSTMENT", nullable = false)
	private int reportCountAdjustment = 0;

	public User(String email, String nickname, String phone) {
		this.email = email;
		this.nickname = nickname;
		this.phone = phone;
		this.regDate = java.time.LocalDateTime.now();
	}

	public User(String loginId, String password, String email, String nickname, String phone, String profileImageUrl) {
		this.loginId = loginId;
		this.password = password;
		this.email = email;
		this.nickname = nickname;
		this.phone = phone;
		this.profileImageUrl = profileImageUrl;
		this.regDate = java.time.LocalDateTime.now();
	}

	public void updateProfileImage(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void withdraw() {
		this.status = "WITHDRAWN";
		this.withdrawnAt = java.time.LocalDateTime.now();
	}

	public void setStatus(String status) {
		this.status = status;
		if ("WITHDRAWN".equals(status)) {
			if (this.withdrawnAt == null) {
				this.withdrawnAt = java.time.LocalDateTime.now();
			}
		} else {
			this.withdrawnAt = null;
		}
	}

	public void setReportCountAdjustment(int reportCountAdjustment) {
		this.reportCountAdjustment = reportCountAdjustment;
	}

	@PrePersist
	public void onCreate() {
		if (this.regDate == null) {
			this.regDate = java.time.LocalDateTime.now();
		}
	}
}
