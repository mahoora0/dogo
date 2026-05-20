package com.example.dogo.service.missing.client;

import com.example.dogo.service.missing.parser.Safe182MissingPersonXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Component
public class DataSafe182MissingPersonClient implements Safe182MissingPersonClient {

	private final RestClient restClient;
	private final Safe182MissingPersonXmlParser parser;
	private final String esntlId;
	private final String authKey;

	public DataSafe182MissingPersonClient(
			@Value("${safe182.missing-search.base-url:https://www.safe182.go.kr/api/lcm/findChildList.do}") String baseUrl,
			@Value("${safe182.missing-search.esntl-id:}") String esntlId,
			@Value("${safe182.missing-search.auth-key:}") String authKey,
			Safe182MissingPersonXmlParser parser
	) {
		this.restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.build();
		this.esntlId = esntlId;
		this.authKey = authKey;
		this.parser = parser;
	}

	@Override
	public Safe182MissingPersonPage search(String keyword, int page, int rowSize) {
		if (!StringUtils.hasText(esntlId) || !StringUtils.hasText(authKey)) {
			throw new IllegalStateException("Safe182 missing search API credentials are not configured.");
		}

		String response = utf8(restClient.post()
				.uri(uriBuilder -> {
					uriBuilder
							.queryParam("esntlId", esntlId)
							.queryParam("authKey", authKey)
							.queryParam("rowSize", rowSize)
							.queryParam("page", page)
							.queryParam("xmlUseYN", "Y");
					if (StringUtils.hasText(keyword)) {
						uriBuilder.queryParam("nm", keyword.trim());
					}
					return uriBuilder.build();
				})
				.retrieve()
				.body(byte[].class));

		Safe182MissingPersonPage result = parser.parse(response);
		if (StringUtils.hasText(result.result()) && !"00".equals(result.result())) {
			throw new IllegalStateException("Safe182 missing search API call failed: " + result.result() + " " + result.message());
		}
		return result;
	}

	private String utf8(byte[] response) {
		if (response == null || response.length == 0) {
			return "";
		}
		return new String(response, StandardCharsets.UTF_8);
	}
}
