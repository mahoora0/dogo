package com.example.dogo.entity;

public enum ReportTargetType {
	LOST_ITEM("분실물"),
	FOUND_ITEM("습득물"),
	ANIMAL_REPORT("동물 신고"),
	MISSING_PERSON("실종자 신고"),
	CHAT_MESSAGE("채팅 메시지");

	private final String label;

	ReportTargetType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
