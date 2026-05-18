package com.example.dogo.dto.missing;

import java.util.List;

public record Safe182AmberAlertPage(
		String resultCode,
		String resultMessage,
		int totalCount,
		List<Safe182AmberAlertView> alerts
) {
	public Safe182AmberAlertPage {
		alerts = List.copyOf(alerts);
	}
}
