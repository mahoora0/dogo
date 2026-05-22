package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DataGoKrAnimalLossApiClient implements AnimalLossApiClient {

	private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient restClient;
	private final AnimalPublicApiXmlParser parser;
	private final String serviceKey;

	public DataGoKrAnimalLossApiClient(
			@Value("${animal-loss.base-url}") String baseUrl,
			@Value("${animal-loss.service-key:}") String serviceKey,
			AnimalPublicApiXmlParser parser
	) {
		this.restClient = RestClient.builder().baseUrl(baseUrl).build();
		this.serviceKey = serviceKey;
		this.parser = parser;
	}

	@Override
	public AnimalPublicApiPage fetch(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows) {
		if (!StringUtils.hasText(serviceKey)) {
			throw new IllegalStateException("Animal loss API service key is not configured.");
		}
		String response = utf8(restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/lossInfo")
						.queryParam("serviceKey", serviceKey)
						.queryParam("bgnde", startDate.format(REQUEST_DATE_FORMAT))
						.queryParam("ended", endDate.format(REQUEST_DATE_FORMAT))
						.queryParam("pageNo", pageNo)
						.queryParam("numOfRows", numOfRows)
						.queryParam("_type", "xml")
						.build())
				.retrieve()
				.body(byte[].class));
		AnimalPublicApiPage page = parser.parse(response);
		if (StringUtils.hasText(page.resultCode()) && !"00".equals(page.resultCode())) {
			throw new IllegalStateException("Animal loss API call failed: " + page.resultCode() + " " + page.resultMessage());
		}
		return page;
	}

	private String utf8(byte[] response) {
		if (response == null || response.length == 0) {
			return "";
		}
		return new String(response, StandardCharsets.UTF_8);
	}
}
