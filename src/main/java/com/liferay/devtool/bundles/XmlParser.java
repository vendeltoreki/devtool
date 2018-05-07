package com.liferay.devtool.bundles;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlParser {
	public static void main(String[] args) throws Exception {
		XmlParser p = new XmlParser();
		p.readFile("C:\\Users\\Liferay\\bundles\\tomcat-8.0.32\\conf\\server.xml");
	}

	public void readFile(String path) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new File(path));

		parseNode(document.getDocumentElement());
	}

	private void parseNode(Element documentElement) {
		// System.out.println("E:" + documentElement);

		NamedNodeMap attributes = documentElement.getAttributes();
		for (int i = 0; i < attributes.getLength(); ++i) {
			Node item = attributes.item(i);
			if (item.getNodeName().trim().toLowerCase().endsWith("port")) {
				System.out.println(documentElement.getTagName() + " " + item.getNodeName() + "=" + item.getNodeValue());
			}
		}

		NodeList nodeList = documentElement.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) node;
				parseNode(elem);
			}
		}
	}

}
