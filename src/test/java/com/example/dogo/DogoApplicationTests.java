package com.example.dogo;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

class DogoApplicationTests {

	@Test
	void testRealApiCall() {
		String baseUrl = "https://apis.data.go.kr/1543061/lossInfoService";
		String serviceKey = "b8cde13e84cf9c88d0521a64c7eef2c989588d68922d74bbfb2e85a5254fb640";
		
		RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
		LocalDate startDate = LocalDate.now().minusDays(90);
		LocalDate endDate = LocalDate.now();
		
		DateTimeFormatter format = DateTimeFormatter.BASIC_ISO_DATE;
		
		try {
			String response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/lossInfo")
							.queryParam("serviceKey", serviceKey)
							.queryParam("bgnde", startDate.format(format))
							.queryParam("ended", endDate.format(format))
							.queryParam("pageNo", 1)
							.queryParam("numOfRows", 10)
							.queryParam("_type", "xml")
							.build())
					.retrieve()
					.body(String.class);
			
			System.out.println("====== REAL API CALL RESPONSE ======");
			System.out.println(response);
			System.out.println("====================================");
		} catch (Exception e) {
			System.out.println("====== REAL API CALL FAILED ======");
			e.printStackTrace();
		}
	}

}
