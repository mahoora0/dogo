package com.example.dogo.dto;

import com.example.dogo.entity.ReportReasonType;
import com.example.dogo.entity.ReportTargetType;

public class ReportCreateRequest {

	private ReportTargetType targetType;
	private Long targetId;
	private ReportReasonType reasonType;
	private String reasonDetail;

	public ReportTargetType getTargetType() {
		return targetType;
	}

	public void setTargetType(ReportTargetType targetType) {
		this.targetType = targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	public ReportReasonType getReasonType() {
		return reasonType;
	}

	public void setReasonType(ReportReasonType reasonType) {
		this.reasonType = reasonType;
	}

	public String getReasonDetail() {
		return reasonDetail;
	}

	public void setReasonDetail(String reasonDetail) {
		this.reasonDetail = reasonDetail;
	}
}
