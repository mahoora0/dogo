package com.example.dogo.service.police.parser;

import com.example.dogo.dto.police.PoliceRegionCode;
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
public class PoliceCommonCodeXmlParser {

	public List<PoliceRegionCode> parseRegionCodes(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return List.of();
		}

		List<RawRegionCode> rawCodes = rawRegionCodes(document.getElementsByTagName("item"));
		return rawCodes.stream()
				.filter(code -> StringUtils.hasText(code.code()))
				.map(code -> new PoliceRegionCode(code.code(), regionName(code, rawCodes)))
				.filter(code -> StringUtils.hasText(code.name()))
				.toList();
	}

	public String resultCode(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return null;
		}
		return text(document, "resultCode");
	}

	public String resultMessage(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return null;
		}
		return firstText(document, "resultMsg", "resultMag");
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
			throw new IllegalArgumentException("경찰청 공통코드 응답 XML 파싱에 실패했습니다.", exception);
		}
	}

	private List<RawRegionCode> rawRegionCodes(NodeList itemNodes) {
		List<RawRegionCode> responses = new ArrayList<>();
		for (int index = 0; index < itemNodes.getLength(); index++) {
			Node node = itemNodes.item(index);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element item = (Element) node;
			responses.add(new RawRegionCode(
					text(item, "commCd"),
					text(item, "cdNm")
			));
		}
		return responses;
	}

	private String regionName(RawRegionCode code, List<RawRegionCode> allCodes) {
		String normalizedName = blankToNull(code.name());
		if (normalizedName == null) {
			return null;
		}
		if (isTopLevelCode(code.code())) {
			return normalizedName;
		}

		String topLevelName = allCodes.stream()
				.filter(candidate -> topLevelCode(code.code()).equals(candidate.code()))
				.map(RawRegionCode::name)
				.map(this::blankToNull)
				.filter(StringUtils::hasText)
				.findFirst()
				.orElse(null);
		if (!StringUtils.hasText(topLevelName)) {
			return normalizedName;
		}
		return topLevelName + " " + normalizedName;
	}

	private boolean isTopLevelCode(String code) {
		return StringUtils.hasText(code) && code.endsWith("000");
	}

	private String topLevelCode(String code) {
		if (!StringUtils.hasText(code) || code.length() < 3) {
			return code;
		}
		return code.substring(0, 3) + "000";
	}

	private String firstText(Document document, String firstTagName, String secondTagName) {
		String first = text(document, firstTagName);
		if (StringUtils.hasText(first)) {
			return first;
		}
		return text(document, secondTagName);
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

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private record RawRegionCode(String code, String name) {
	}
}
