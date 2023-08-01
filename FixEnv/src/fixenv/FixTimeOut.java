package fixenv;

import org.w3c.dom.Node;

import fixenv.util.FileUtil;
import fixenv.util.XmlParser;

public class FixTimeOut {
	private String bundleRoot;
	
	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void execute() {
		updateTimeout(FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/conf/web.xml"), 1440);
		updateTimeout(FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/webapps/ROOT/WEB-INF/web.xml"), 1440);
	}
	
	public void updateTimeout(String filePath, int value) {
		System.out.println("Updating session timeout in file: "+filePath+", to: "+value);
		
		XmlParser xmlParser = new XmlParser();
		xmlParser.readFile(filePath);
		Node node = xmlParser.getNodeByXPath("/web-app/session-config/session-timeout");
		
		if (node != null) {
			String currentValueStr = node.getTextContent();
			int currentValue = Integer.valueOf(currentValueStr);
			
			if (currentValue < value) {
				System.out.println("Changing value "+currentValue+" to "+value);
				node.setTextContent(String.valueOf(value));
				
				xmlParser.saveFile();
			} else {
				System.out.println("Current value is ok: "+currentValue);
			}
		}
		
	}

	public void info() {
		System.out.println("\n-- Session timeout settings -------------------------");
		
		readTimeout(FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/conf/web.xml"));
		readTimeout(FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/webapps/ROOT/WEB-INF/web.xml"));
	}

	private void readTimeout(String filePath) {
		System.out.println("Reading session timeout in file: "+filePath);
		
		XmlParser xmlParser = new XmlParser();
		xmlParser.readFile(filePath);
		Node node = xmlParser.getNodeByXPath("/web-app/session-config/session-timeout");
		
		if (node != null) {
			String currentValueStr = node.getTextContent();
			int currentValue = Integer.valueOf(currentValueStr);
			
			System.out.println("Current value: "+currentValue);
		}
	}

}
