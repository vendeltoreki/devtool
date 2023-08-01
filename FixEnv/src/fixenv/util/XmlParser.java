package fixenv.util;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
	private String filePath;
	
	public void readFile(String path) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(new File(path));
			filePath = path;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveFile() {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);		
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
