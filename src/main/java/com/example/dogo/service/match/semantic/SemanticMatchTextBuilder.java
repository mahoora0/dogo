package com.example.dogo.service.match.semantic;

import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.entity.item.LostItem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SemanticMatchTextBuilder {

	public static final String TEXT_VERSION = "semantic-text-v2";

	public String build(SemanticMatchItem item) {
		if (item == null) {
			return "";
		}
		List<String> parts = new ArrayList<>();
		append(parts, "물품명", item.itemName());
		append(parts, "제목", item.title());
		append(parts, "카테고리", item.category());
		append(parts, "색상", item.color());
		return String.join(". ", parts);
	}

	public String build(LostItem item) {
		if (item == null) {
			return "";
		}
		return build(new SemanticMatchItem(
				item.getLostId(),
				"LOST",
				item.getItemName(),
				item.getTitle(),
				item.getCategoryMain(),
				item.getColorName(),
				item.getLostArea(),
				item.getLostPlace(),
				item.getContent()
		));
	}

	public String build(FoundItem item) {
		if (item == null) {
			return "";
		}
		return build(new SemanticMatchItem(
				item.getFoundId(),
				"FOUND",
				item.getItemName(),
				item.getTitle(),
				item.getCategoryMain(),
				item.getColorName(),
				item.getFoundArea(),
				item.getFoundPlace(),
				item.getContent()
		));
	}

	public String hash(String text) {
		return sha256(TEXT_VERSION + ":" + (text == null ? "" : text));
	}

	private void append(List<String> parts, String label, String value) {
		if (!StringUtils.hasText(value)) {
			return;
		}

		String normalized = value.trim().replaceAll("\\s+", " ");
		boolean duplicate = parts.stream()
				.anyMatch(existing -> existing.endsWith(": " + normalized));
		if (!duplicate) {
			parts.add(label + ": " + normalized);
		}
	}

	private String sha256(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(64);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
