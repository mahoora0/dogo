package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemPage;
import com.example.dogo.dto.PoliceFoundItemResponse;
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
import java.util.Optional;

@Component
public class PoliceFoundItemXmlParser {

	public PoliceFoundItemPage parse(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return new PoliceFoundItemPage(null, null, 0, List.of());
		}

		return new PoliceFoundItemPage(
				text(document, "resultCode"),
				firstText(document, "resultMsg", "resultMag"),
				parseInt(text(document, "totalCount")),
				items(document.getElementsByTagName("item"))
		);
	}

	public Optional<PoliceFoundItemDetailResponse> parseDetail(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return Optional.empty();
		}

		NodeList itemNodes = document.getElementsByTagName("item");
		if (itemNodes.getLength() == 0 || itemNodes.item(0).getNodeType() != Node.ELEMENT_NODE) {
			return Optional.empty();
		}

		Element item = (Element) itemNodes.item(0);
		return Optional.of(new PoliceFoundItemDetailResponse(
				text(item, "atcId"),
				text(item, "csteSteNm"),
				text(item, "depPlace"),
				text(item, "fdFilePathImg"),
				text(item, "fdHor"),
				text(item, "fdPlace"),
				text(item, "fdPrdtNm"),
				text(item, "fdSn"),
				text(item, "fdYmd"),
				text(item, "fndKeepOrgnSeNm"),
				text(item, "orgId"),
				text(item, "orgNm"),
				text(item, "prdtClNm"),
				text(item, "tel"),
				text(item, "uniq"),
				text(item, "clrNm")
		));
	}

	String resultCode(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return null;
		}
		return text(document, "resultCode");
	}

	String resultMessage(String xml) {
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
			throw new IllegalArgumentException("경찰청 습득물 응답 XML 파싱에 실패했습니다.", exception);
		}
	}

	private List<PoliceFoundItemResponse> items(NodeList itemNodes) {
		List<PoliceFoundItemResponse> responses = new ArrayList<>();
		for (int index = 0; index < itemNodes.getLength(); index++) {
			Node node = itemNodes.item(index);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element item = (Element) node;
			PoliceFoundItemResponse response = new PoliceFoundItemResponse(
					text(item, "atcId"),
					text(item, "clrNm"),
					text(item, "depPlace"),
					text(item, "fdFilePathImg"),
					text(item, "fdPrdtNm"),
					text(item, "fdSbjt"),
					text(item, "fdSn"),
					text(item, "fdYmd"),
					text(item, "prdtClNm")
			);
			if (StringUtils.hasText(response.atcId())) {
				responses.add(response);
			}
		}
		return responses;
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
		return value.trim();
	}
}
