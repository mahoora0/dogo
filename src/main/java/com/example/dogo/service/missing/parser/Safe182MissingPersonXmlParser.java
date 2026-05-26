package com.example.dogo.service.missing.parser;

import com.example.dogo.service.missing.client.Safe182MissingPersonPage;
import com.example.dogo.service.missing.client.Safe182MissingPersonRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class Safe182MissingPersonXmlParser {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	public Safe182MissingPersonPage parse(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return new Safe182MissingPersonPage(null, null, 0, List.of());
		}

		return new Safe182MissingPersonPage(
				firstText(document, "result", "resultCode"),
				firstText(document, "msg", "resultMsg"),
				parseInt(text(document, "totalCount")),
				records(document, xml)
		);
	}

	private List<Safe182MissingPersonRecord> records(Document document, String xml) {
		NodeList nodes = document.getElementsByTagName("item");
		if (nodes.getLength() == 0) {
			nodes = document.getElementsByTagName("list");
		}
		List<Safe182MissingPersonRecord> records = new ArrayList<>();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element element = (Element) node;
			if (element.getElementsByTagName("occrde").getLength() == 0
					&& element.getElementsByTagName("nm").getLength() == 0) {
				continue;
			}
			records.add(record(element, elementToString(element)));
		}
		return records;
	}

	private Safe182MissingPersonRecord record(Element element, String elementXml) {
		String name = text(element, "nm");
		String occurredDate = text(element, "occrde");
		String gender = text(element, "sexdstnDscd");
		String age = text(element, "age");
		String place = text(element, "occrAdres");
		String key = String.join("|",
				value(name),
				value(occurredDate),
				value(gender),
				value(age),
				value(place)
		);

		return new Safe182MissingPersonRecord(
				hash(key),
				name,
				gender,
				parseInteger(age),
				parseDate(occurredDate),
				place,
				parseInteger(text(element, "height")),
				parseDecimal(text(element, "bdwgh")),
				text(element, "frmDscd"),
				text(element, "faceshpeDscd"),
				text(element, "haircolrDscd"),
				text(element, "hairshpeDscd"),
				text(element, "alldressingDscd"),
				elementXml
		);
	}

	private String elementToString(Element element) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(element), new StreamResult(writer));
			return writer.getBuffer().toString();
		} catch (Exception e) {
			return "";
		}
	}

	private Document parseDocument(String xml) {
		if (!StringUtils.hasText(xml)) {
			return null;
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
			document.getDocumentElement().normalize();
			return document;
		} catch (Exception exception) {
			throw new IllegalArgumentException("Safe182 missing search XML response could not be parsed.", exception);
		}
	}

	private String firstText(Document document, String firstTagName, String secondTagName) {
		String first = text(document, firstTagName);
		if (StringUtils.hasText(first)) {
			return first;
		}
		return text(document, secondTagName);
	}

	private String firstText(Element element, String firstTagName, String secondTagName) {
		String first = text(element, firstTagName);
		if (StringUtils.hasText(first)) {
			return first;
		}
		return text(element, secondTagName);
	}

	private String text(Document document, String tagName) {
		NodeList nodes = document.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return null;
		}
		return blankToNull(nodes.item(0).getTextContent());
	}

	private String text(Element element, String tagName) {
		NodeList nodes = element.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return null;
		}
		return blankToNull(nodes.item(0).getTextContent());
	}

	private LocalDateTime parseDate(String value) {
		if (!StringUtils.hasText(value)) {
			return LocalDateTime.now();
		}
		return LocalDate.parse(value.trim(), DATE_FORMAT).atStartOfDay();
	}

	private Integer parseInteger(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return Integer.parseInt(value.trim());
	}

	private int parseInt(String value) {
		Integer parsed = parseInteger(value);
		return parsed != null ? parsed : 0;
	}

	private BigDecimal parseDecimal(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return new BigDecimal(value.trim());
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed).substring(0, 32);
		} catch (Exception exception) {
			throw new IllegalStateException("Safe182 external id could not be generated.", exception);
		}
	}

	private String value(String value) {
		return StringUtils.hasText(value) ? value.trim() : "";
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.equalsIgnoreCase("null")) {
			return null;
		}
		return trimmed;
	}
}
