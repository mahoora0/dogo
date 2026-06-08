package com.example.dogo.controller.common;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

public final class ListBackUrlBuilder {

	private final UriComponentsBuilder builder;

	private ListBackUrlBuilder(String path) {
		this.builder = UriComponentsBuilder.fromPath(path);
	}

	public static ListBackUrlBuilder fromPath(String path) {
		return new ListBackUrlBuilder(path);
	}

	public ListBackUrlBuilder queryParam(String name, Object value) {
		if (value instanceof String text) {
			if (StringUtils.hasText(text)) {
				builder.queryParam(name, text);
			}
			return this;
		}
		if (value != null) {
			builder.queryParam(name, value);
		}
		return this;
	}

	public String build() {
		return builder.build().encode().toUriString();
	}
}
