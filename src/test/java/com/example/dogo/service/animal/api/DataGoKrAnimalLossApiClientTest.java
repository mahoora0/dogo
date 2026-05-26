package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiPage;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DataGoKrAnimalLossApiClientTest {

	@Test
	void fetchCallsLossInfoOperationWithEndedParameter() throws Exception {
		try (TestApiServer server = new TestApiServer()) {
			DataGoKrAnimalLossApiClient client = new DataGoKrAnimalLossApiClient(
					server.baseUrl(),
					"test-key",
					new AnimalPublicApiXmlParser()
			);

			AnimalPublicApiPage page = client.fetch(
					LocalDate.of(2026, 5, 1),
					LocalDate.of(2026, 5, 22),
					1,
					10
			);

			assertThat(page.resultCode()).isEqualTo("00");
			assertThat(server.requestPath).isEqualTo("/lossInfo");
			assertThat(server.requestQuery).contains("bgnde=20260501", "ended=20260522");
			assertThat(server.requestQuery).doesNotContain("endde=");
		}
	}

	private static class TestApiServer implements AutoCloseable {
		private final HttpServer server;
		private String requestPath;
		private String requestQuery;

		TestApiServer() throws IOException {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/", exchange -> {
				requestPath = exchange.getRequestURI().getPath();
				requestQuery = exchange.getRequestURI().getQuery();
				boolean valid = "/lossInfo".equals(requestPath)
						&& requestQuery != null
						&& requestQuery.contains("bgnde=20260501")
						&& requestQuery.contains("ended=20260522")
						&& !requestQuery.contains("endde=");
				byte[] body = (valid ? successXml() : "Unexpected errors").getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(valid ? 200 : 500, body.length);
				exchange.getResponseBody().write(body);
				exchange.close();
			});
			server.start();
		}

		String baseUrl() {
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		private static String successXml() {
			return """
					<response>
					  <header><resultCode>00</resultCode><resultMsg>OK</resultMsg></header>
					  <body><items></items><totalCount>0</totalCount></body>
					</response>
					""";
		}

		@Override
		public void close() {
			server.stop(0);
		}
	}
}
