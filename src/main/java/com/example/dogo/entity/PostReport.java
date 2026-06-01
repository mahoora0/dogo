package com.example.dogo.entity;

import com.example.dogo.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
		name = "POST_REPORT",
		uniqueConstraints = {
				@UniqueConstraint(name = "UK_REPORT_DUPLICATE", columnNames = {"REPORTER_NO", "TARGET_TYPE", "TARGET_ID"})
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "REPORT_ID")
	private Long reportId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "REPORTER_NO", nullable = false)
	private User reporter;

	@Enumerated(EnumType.STRING)
	@Column(name = "TARGET_TYPE", nullable = false, length = 30)
	private ReportTargetType targetType;

	@Column(name = "TARGET_ID", nullable = false)
	private Long targetId;

	@Column(name = "TARGET_OWNER_NO", nullable = false)
	private Long targetOwnerNo;

	@Column(name = "TARGET_TITLE", nullable = false, length = 300)
	private String targetTitle;

	@Column(name = "TARGET_URL", nullable = false, length = 500)
	private String targetUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "REASON_TYPE", nullable = false, length = 30)
	private ReportReasonType reasonType;

	@Column(name = "REASON_DETAIL", length = 1000)
	private String reasonDetail;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS", nullable = false, length = 30)
	private ReportStatus status = ReportStatus.PENDING;

	@Column(name = "ADMIN_MEMO", length = 1000)
	private String adminMemo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "HANDLER_NO")
	private User handler;

	@Column(name = "CREATED_AT", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "HANDLED_AT")
	private LocalDateTime handledAt;

	public PostReport(
			User reporter,
			ReportTargetType targetType,
			Long targetId,
			Long targetOwnerNo,
			String targetTitle,
			String targetUrl,
			ReportReasonType reasonType,
			String reasonDetail
	) {
		this.reporter = reporter;
		this.targetType = targetType;
		this.targetId = targetId;
		this.targetOwnerNo = targetOwnerNo;
		this.targetTitle = targetTitle;
		this.targetUrl = targetUrl;
		this.reasonType = reasonType;
		this.reasonDetail = reasonDetail;
	}

	public void updateStatus(ReportStatus status, String adminMemo, User handler) {
		this.status = status;
		this.adminMemo = adminMemo;
		this.handler = handler;
		this.handledAt = LocalDateTime.now();
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
