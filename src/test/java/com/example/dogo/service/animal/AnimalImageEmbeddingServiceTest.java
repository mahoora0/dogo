package com.example.dogo.service.animal;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AnimalImageEmbeddingServiceTest {

	@Test
	void externalImageUriEncodesPublicApiBracketFilenames() {
		URI uri = AnimalImageEmbeddingService.externalImageUri(
				"http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/05/202606041706321[1].jpg"
		);

		assertThat(uri.toString()).endsWith("202606041706321%5B1%5D.jpg");
	}
}
