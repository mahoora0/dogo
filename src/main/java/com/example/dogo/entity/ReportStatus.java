package com.example.dogo.entity;

public enum ReportStatus {
	PENDING("접수됨"),
	REVIEWING("검토중"),
	RESOLVED("조치완료"),
	REJECTED("반려");

	private final String label;

	ReportStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
