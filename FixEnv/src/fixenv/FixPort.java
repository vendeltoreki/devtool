package fixenv;

import org.w3c.dom.Node;

import fixenv.util.FileUtil;
import fixenv.util.XmlParser;

public class FixPort {
	private String bundleRoot;
	private XmlParser xmlParser = new XmlParser();
	private boolean changed = false;
	private String portPrefix = "90";

	private String[] xpaths = new String[] {
		"/Server/@port",
		"/Server/Service[@name='Catalina']/Connector[@protocol='HTTP/1.1']/@port",
		"/Server/Service[@name='Catalina']/Connector[@protocol='HTTP/1.1']/@redirectPort",
		"/Server/Service[@name='Catalina']/Connector[@protocol='AJP/1.3']/@port",
		"/Server/Service[@name='Catalina']/Connector[@protocol='AJP/1.3']/@redirectPort"
	};
	
	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void setPortPrefix(String portPrefix) {
		this.portPrefix = portPrefix;
	}

	public void execute() {
		String filePath = FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/conf/server.xml");
		
		System.out.println("Updating values in file: "+filePath);
		
		xmlParser.readFile(filePath);
		
		//updateIntValue("/Server/@port", 9005);
		//updateIntValue("/Server/Service[@name='Catalina']/Connector[@protocol='HTTP/1.1']/@port", 9080);
		
		for (String xpath : xpaths) {
			updatePortValueWithPrefix(xpath);
		}

		if (changed) {
			xmlParser.saveFile();
		}
	}

	public void info() {
		System.out.println("\n-- Ports --------------------------------------------");
		
		String filePath = FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/conf/server.xml");
		
		System.out.println("Values in file: "+filePath);
		
		xmlParser.readFile(filePath);
		
		for (String xpath : xpaths) {
			readIntValue(xpath);
		}
	}

	public void updateIntValue(String xPath, int value) {
		Node node = xmlParser.getNodeByXPath(xPath);
		
		if (node != null) {
			System.out.println("Node \""+xPath+"\" found");
			
			String currentValueStr = node.getNodeValue();
			int currentValue = Integer.valueOf(currentValueStr);
			
			if (currentValue != value) {
				System.out.println("Changing value "+currentValue+" to "+value);
				node.setNodeValue(String.valueOf(value));

				changed = true;
			} else {
				System.out.println("Current value is ok: "+currentValue);
			}
		} else {
			System.out.println("Node \""+xPath+"\" not found!");
		}
		
	}

	public void updatePortValueWithPrefix(String xPath) {
		Node node = xmlParser.getNodeByXPath(xPath);
		
		if (node != null) {
			System.out.println("Node \""+xPath+"\" found");
			
			String currentValue = node.getNodeValue();
			
			String newValue = calculatePortValue(currentValue, portPrefix);
			
			if (!newValue.equals(currentValue)) {
				System.out.println("Changing value "+currentValue+" to "+newValue);
				node.setNodeValue(String.valueOf(newValue));

				changed = true;
			} else {
				System.out.println("Current value is ok: "+currentValue);
			}
		} else {
			System.out.println("Node \""+xPath+"\" not found!");
		}
		
	}
	
	private String calculatePortValue(String value, String prefix) {
		if (prefix.length() == 2 && prefix.charAt(1) == '0') {
			prefix = prefix.substring(0,1);
		}

		if (value.startsWith(prefix)) {
			return value;
		} else {
			if (prefix.length() == 1) {
				return prefix + value.substring(1);
			} else if (prefix.length() == 2) {
				return prefix + value.substring(2);
			} else {
				return value;
			}
			
		}
	}

	public void readIntValue(String xPath) {
		Node node = xmlParser.getNodeByXPath(xPath);
		
		if (node != null) {
			String currentValueStr = node.getNodeValue();
			System.out.println("Value of \""+xPath+"\": "+currentValueStr);
		} else {
			System.out.println("Node \""+xPath+"\" not found!");
		}
	}
}
