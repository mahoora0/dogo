package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiPage;
import com.example.dogo.dto.animal.AnimalPublicApiRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnimalPublicApiXmlParser {

	public AnimalPublicApiPage parse(String xml) {
		Document document = parseDocument(xml);
		if (document == null) {
			return new AnimalPublicApiPage(null, null, 0, List.of());
		}

		return new AnimalPublicApiPage(
				text(document, "resultCode"),
				firstText(document, "resultMsg", "resultMessage"),
				parseInt(text(document, "totalCount")),
				items(document.getElementsByTagName("item"))
		);
	}

	private List<AnimalPublicApiRecord> items(NodeList itemNodes) {
		List<AnimalPublicApiRecord> records = new ArrayList<>();
		for (int index = 0; index < itemNodes.getLength(); index++) {
			Node node = itemNodes.item(index);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element item = (Element) node;
			String externalId = firstText(item, "desertionNo", "lossNo", "noticeNo", "id");
			String imageUrl = firstText(item, "popfile", "filename", "imageUrl", "photoUrl");

			if (!StringUtils.hasText(externalId)) {
				if (StringUtils.hasText(imageUrl)) {
					int lastSlash = imageUrl.lastIndexOf('/');
					if (lastSlash != -1 && lastSlash < imageUrl.length() - 1) {
						String filename = imageUrl.substring(lastSlash + 1);
						int lastDot = filename.lastIndexOf('.');
						if (lastDot != -1) {
							filename = filename.substring(0, lastDot);
						}
						if (StringUtils.hasText(filename)) {
							externalId = filename.trim();
						}
					}
				}
				if (!StringUtils.hasText(externalId)) {
					String date = firstText(item, "happenDt", "lossDate", "happenDate", "noticeDate");
					String tel = firstText(item, "careTel", "tel", "officetel");
					String kind = firstText(item, "kindFullNm", "kindCd", "kindName", "upKindNm", "kindNm");
					String place = firstText(item, "happenPlace", "lossPlace", "place", "addr");
					String combination = (date != null ? date : "") + "_" + (tel != null ? tel : "") + "_" + (kind != null ? kind : "") + "_" + (place != null ? place : "");
					externalId = "SYNTHETIC-" + Math.abs(combination.hashCode());
				}
			}

			AnimalPublicApiRecord record = new AnimalPublicApiRecord(
					externalId,
					firstText(item, "happenDt", "lossDate", "happenDate", "noticeDate"),
					firstText(item, "happenPlace", "lossPlace", "place", "addr"),
					firstText(item, "careAddr", "orgNm", "orgName", "regionName"),
					firstText(item, "careTel", "tel", "officetel"),
					firstText(item, "kindFullNm", "kindCd", "kindName", "upKindNm", "kindNm"),
					firstText(item, "breedNm", "breedName", "kindNm"),
					firstText(item, "colorCd", "color", "furColor"),
					firstText(item, "sexCd", "sex", "gender"),
					firstText(item, "neuterYn", "neuter", "neutered"),
					firstText(item, "age", "ageText"),
					firstText(item, "weight", "weightText"),
					firstText(item, "specialMark", "feature", "distinctiveMarks"),
					firstText(item, "processState", "state", "status"),
					imageUrl,
					toXml(item)
			);
			if (StringUtils.hasText(record.externalId())) {
				records.add(record);
			}
		}
		return records;
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
			throw new IllegalArgumentException("Animal public API XML parsing failed.", exception);
		}
	}

	private String firstText(Document document, String... tagNames) {
		for (String tagName : tagNames) {
			String value = text(document, tagName);
			if (StringUtils.hasText(value)) {
				return value;
			}
		}
		return null;
	}

	private String firstText(Element element, String... tagNames) {
		for (String tagName : tagNames) {
			String value = text(element, tagName);
			if (StringUtils.hasText(value)) {
				return value;
			}
		}
		return null;
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

	private String toXml(Node node) {
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			var transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(node), new StreamResult(writer));
			return writer.toString();
		} catch (Exception exception) {
			return null;
		}
	}

	private String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
