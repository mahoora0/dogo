package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemDetailResponse;
import com.example.dogo.dto.PoliceLostItemPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class DataGoKrPoliceLostItemClient implements PoliceLostItemClient {

	private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient listRestClient;
	private final RestClient detailRestClient;
	private final PoliceLostItemXmlParser parser;
	private final String serviceKey;

	public DataGoKrPoliceLostItemClient(
			@Value("${police.lost-item.base-url}") String baseUrl,
			@Value("${police.lost-item.detail-url}") String detailUrl,
			@Value("${police.lost-item.service-key:}") String serviceKey,
			PoliceLostItemXmlParser parser
	) {
		this.listRestClient = RestClient.builder()
				.baseUrl(baseUrl)
				.build();
		this.detailRestClient = RestClient.builder()
				.baseUrl(detailUrl)
				.build();
		this.serviceKey = serviceKey;
		this.parser = parser;
	}

	@Override
	public PoliceLostItemPage fetchLostItems(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows) {
		if (!StringUtils.hasText(serviceKey)) {
			throw new IllegalStateException("경찰청 분실물 API serviceKey가 설정되지 않았습니다.");
		}

		String response = utf8(listRestClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("serviceKey", serviceKey)
						.queryParam("START_YMD", startDate.format(REQUEST_DATE_FORMAT))
						.queryParam("END_YMD", endDate.format(REQUEST_DATE_FORMAT))
						.queryParam("pageNo", pageNo)
						.queryParam("numOfRows", numOfRows)
						.build())
				.retrieve()
				.body(byte[].class));

		PoliceLostItemPage page = parser.parse(response);
		if (StringUtils.hasText(page.resultCode()) && !"00".equals(page.resultCode())) {
			throw new IllegalStateException("경찰청 분실물 API 호출 실패: " + page.resultCode() + " " + page.resultMessage());
		}
		return page;
	}

	@Override
	public Optional<PoliceLostItemDetailResponse> fetchLostItemDetail(String atcId) {
		if (!StringUtils.hasText(serviceKey)) {
			throw new IllegalStateException("경찰청 분실물 API serviceKey가 설정되지 않았습니다.");
		}
		if (!StringUtils.hasText(atcId)) {
			return Optional.empty();
		}

		String response = utf8(detailRestClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("serviceKey", serviceKey)
						.queryParam("ATC_ID", atcId.trim())
						.build())
				.retrieve()
				.body(byte[].class));

		String resultCode = parser.resultCode(response);
		if (StringUtils.hasText(resultCode) && !"00".equals(resultCode)) {
			throw new IllegalStateException("경찰청 분실물 상세 API 호출 실패: " + resultCode + " " + parser.resultMessage(response));
		}
		return parser.parseDetail(response);
	}

	private String utf8(byte[] response) {
		if (response == null || response.length == 0) {
			return "";
		}
		return new String(response, StandardCharsets.UTF_8);
	}
}
