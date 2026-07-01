package com.example.dogo.service.police.client;

import com.example.dogo.dto.police.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.police.PoliceFoundItemPage;
import com.example.dogo.service.police.parser.PoliceFoundItemXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class DataGoKrPoliceFoundItemClient implements PoliceFoundItemClient {

	private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient listRestClient;
	private final RestClient detailRestClient;
	private final PoliceFoundItemXmlParser parser;
	private final String serviceKey;

	public DataGoKrPoliceFoundItemClient(
			@Value("${police.found-item.base-url}") String baseUrl,
			@Value("${police.found-item.detail-url}") String detailUrl,
			@Value("${police.found-item.service-key:}") String serviceKey,
			PoliceFoundItemXmlParser parser
	) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(15));
		factory.setReadTimeout(Duration.ofSeconds(15));

		this.listRestClient = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(factory)
				.build();
		this.detailRestClient = RestClient.builder()
				.baseUrl(detailUrl)
				.requestFactory(factory)
				.build();
		this.serviceKey = serviceKey;
		this.parser = parser;
	}

	@Override
	public PoliceFoundItemPage fetchFoundItems(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows, String regionCode) {
		if (!StringUtils.hasText(serviceKey)) {
			throw new IllegalStateException("경찰청 습득물 API serviceKey가 설정되지 않았습니다.");
		}

		String response = utf8(listRestClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("serviceKey", serviceKey)
						.queryParam("START_YMD", startDate.format(REQUEST_DATE_FORMAT))
						.queryParam("END_YMD", endDate.format(REQUEST_DATE_FORMAT))
						.queryParamIfPresent("N_FD_LCT_CD", Optional.ofNullable(blankToNull(regionCode)))
						.queryParam("pageNo", pageNo)
						.queryParam("numOfRows", numOfRows)
						.build())
				.retrieve()
				.body(byte[].class));

		PoliceFoundItemPage page = parser.parse(response);
		if (StringUtils.hasText(page.resultCode()) && !"00".equals(page.resultCode())) {
			throw new IllegalStateException("경찰청 습득물 API 호출 실패: " + page.resultCode() + " " + page.resultMessage());
		}
		return page;
	}

	@Override
	public Optional<PoliceFoundItemDetailResponse> fetchFoundItemDetail(String atcId, Integer fdSn) {
		if (!StringUtils.hasText(serviceKey)) {
			throw new IllegalStateException("경찰청 습득물 API serviceKey가 설정되지 않았습니다.");
		}
		if (!StringUtils.hasText(atcId) || fdSn == null) {
			return Optional.empty();
		}

		String response = utf8(detailRestClient.get()
				.uri(uriBuilder -> uriBuilder
						.queryParam("serviceKey", serviceKey)
						.queryParam("ATC_ID", atcId.trim())
						.queryParam("FD_SN", fdSn)
						.build())
				.retrieve()
				.body(byte[].class));

		String resultCode = parser.resultCode(response);
		if (StringUtils.hasText(resultCode) && !"00".equals(resultCode)) {
			throw new IllegalStateException("경찰청 습득물 상세 API 호출 실패: " + resultCode + " " + parser.resultMessage(response));
		}
		return parser.parseDetail(response);
	}

	private String utf8(byte[] response) {
		if (response == null || response.length == 0) {
			return "";
		}
		return new String(response, StandardCharsets.UTF_8);
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
