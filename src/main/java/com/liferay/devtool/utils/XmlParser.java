package com.liferay.devtool.utils;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlParser {
	private Document document;
	private XPathFactory xPathfactory = XPathFactory.newInstance();
	
	public void readFile(String path) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(new File(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Node getNodeByXPath(String xpathExpression) {
		if (document == null) {
			return null;
		}
		
		Node node = null;
		
		XPath xpath = xPathfactory.newXPath();
		try {
			XPathExpression expr = xpath.compile(xpathExpression);
			node = (Node)expr.evaluate(document, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return node;
	}

	public String getStringByXPath(String xpathExpression) {
		if (document == null) {
			return null;
		}

		String res = null;
		
		XPath xpath = xPathfactory.newXPath();
		try {
			XPathExpression expr = xpath.compile(xpathExpression);
			res = (String)expr.evaluate(document, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}
}
