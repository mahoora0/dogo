package com.example.dogo.service.missing;

import com.example.dogo.dto.missing.Safe182AmberAlertPage;
import com.example.dogo.dto.missing.Safe182AmberAlertView;
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
import java.util.ArrayList;
import java.util.List;

@Component
public class Safe182AmberAlertXmlParser {

	private static final String SOURCE_LABEL = "자료 출처: 경찰청";

	public Safe182AmberAlertPage parse(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return new Safe182AmberAlertPage(null, null, 0, List.of());
		}

		return new Safe182AmberAlertPage(
				text(document, "result"),
				text(document, "msg"),
				parseInt(text(document, "totalCount")),
				alerts(document)
		);
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
			throw new IllegalArgumentException("안전Dream 실종경보 응답 XML 파싱에 실패했습니다.", exception);
		}
	}

	private List<Safe182AmberAlertView> alerts(Document document) {
		NodeList alertNodes = document.getElementsByTagName("item");
		if (alertNodes.getLength() == 0) {
			alertNodes = document.getElementsByTagName("list");
		}

		List<Safe182AmberAlertView> alerts = new ArrayList<>();
		for (int index = 0; index < alertNodes.getLength(); index++) {
			Node node = alertNodes.item(index);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element item = (Element) node;
			alerts.add(new Safe182AmberAlertView(
					formatOccurrenceDate(text(item, "occrde")),
					text(item, "alldressingDscd"),
					text(item, "ageNow"),
					text(item, "age"),
					mapTargetCode(text(item, "writngTrgetDscd")),
					text(item, "sexdstnDscd"),
					text(item, "occrAdres"),
					text(item, "nm"),
					text(item, "height"),
					text(item, "bdwgh"),
					text(item, "frmDscd"),
					text(item, "faceshpeDscd"),
					text(item, "hairshpeDscd"),
					text(item, "haircolrDscd"),
					text(item, "tknphotolength"),
					SOURCE_LABEL
			));
		}
		return alerts;
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

	private int parseInt(String value) {
		if (!StringUtils.hasText(value)) {
			return 0;
		}
		return Integer.parseInt(value.trim());
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

	private String mapTargetCode(String code) {
		if (code == null) {
			return null;
		}
		return switch (code.trim()) {
			case "010" -> "정상아동(18세미만)";
			case "020" -> "가출인";
			case "040" -> "시설보호무연고자";
			case "060" -> "지적장애인";
			case "061" -> "지적장애인(18세미만)";
			case "062" -> "지적장애인(18세이상)";
			case "070" -> "치매질환자";
			case "080" -> "불상(기타)";
			default -> "실종 경보 (" + code + ")";
		};
	}

	private String formatOccurrenceDate(String dateStr) {
		if (dateStr == null || dateStr.trim().length() != 8) {
			return dateStr;
		}
		String clean = dateStr.trim();
		return clean.substring(0, 4) + "." + clean.substring(4, 6) + "." + clean.substring(6, 8);
	}
}
