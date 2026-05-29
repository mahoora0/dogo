package com.example.dogo.entity;

public enum ReportReasonType {
	SPAM("광고/도배"),
	FRAUD("사기 의심"),
	INAPPROPRIATE("부적절한 내용"),
	PRIVACY("개인정보 노출"),
	WRONG_INFO("허위/잘못된 정보"),
	ETC("기타");

	private final String label;

	ReportReasonType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
