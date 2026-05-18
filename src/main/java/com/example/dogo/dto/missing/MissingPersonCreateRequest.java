package com.example.dogo.dto.missing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class MissingPersonCreateRequest {

	private Integer age;
	private String nationality;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime occurredAt;

	private String occurredPlace;
	private Integer heightCm;
	private BigDecimal weightKg;
	private String bodyType;
	private String faceShape;
	private String hairColor;
	private String hairStyle;
	private String clothing;
}
