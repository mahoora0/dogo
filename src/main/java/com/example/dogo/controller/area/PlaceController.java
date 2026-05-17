package com.example.dogo.controller.area;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
public class PlaceController {

  @Value("${kakao.rest-api-key}")
  private String kakaoApiKey;

  @GetMapping("/search")
  public String searchPlace(@RequestParam String query) {
    String url = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" + query;

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "KakaoAK " + kakaoApiKey);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<String> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        entity,
        String.class
    );

    return response.getBody();
  }
}