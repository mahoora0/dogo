package com.example.dogo.sse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SseTemplateJavascriptInlineTest {

	private static final List<String> SSE_DETAIL_TEMPLATES = List.of(
			"src/main/resources/templates/lost-items/detail.html",
			"src/main/resources/templates/found-items/detail.html",
			"src/main/resources/templates/animal-reports/detail.html"
	);

	@Test
	void sseScriptsThatReadMatchingInProgressUseJavascriptInlining() throws IOException {
		for (String template : SSE_DETAIL_TEMPLATES) {
			String html = Files.readString(Path.of(template));
			int eventSourceIndex = html.indexOf("new EventSource(");
			assertTrue(eventSourceIndex > 0, template + " must open an EventSource");

			int scriptStartIndex = html.lastIndexOf("<script", eventSourceIndex);
			int scriptEndIndex = html.indexOf(">", scriptStartIndex);
			String scriptTag = html.substring(scriptStartIndex, scriptEndIndex + 1);

			assertTrue(scriptTag.contains("th:inline=\"javascript\""),
					template + " must render matchingInProgress with th:inline=\"javascript\"");
		}
	}
}
