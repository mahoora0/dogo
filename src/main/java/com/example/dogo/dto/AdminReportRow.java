package com.example.dogo.dto;

import com.example.dogo.entity.ReportReasonType;
import com.example.dogo.entity.ReportStatus;
import com.example.dogo.entity.ReportTargetType;

import java.time.LocalDateTime;

public record AdminReportRow(
		Long reportId,
		ReportStatus status,
		ReportTargetType targetType,
		String targetTitle,
		String targetUrl,
		ReportReasonType reasonType,
		String reasonDetail,
		Long reporterNo,
		String reporterNickname,
		String reporterLoginId,
		LocalDateTime createdAt,
		String adminMemo,
		Long targetOwnerNo,
		String targetOwnerNickname,
		String targetOwnerLoginId,
		long targetOwnerReportCount
) {
}
