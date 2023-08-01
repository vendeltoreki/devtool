package fixenv;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FixWebserver {
	private String bundleRoot;

	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}
	
	public void execute() {
		File bundleRootDir = new File(bundleRoot);
		
		Map<String,Date> webserverNames = new HashMap<>();
		
		for (File subDir : bundleRootDir.listFiles()) {
			if (!subDir.isDirectory()) {
				continue;
			}
			
			if (subDir.getName().toLowerCase().startsWith("tomcat-") ) {
				webserverNames.put(subDir.getName(), new Date(subDir.lastModified()));
			}
		}
		
		if (webserverNames.size() > 1) {
			System.out.println("Multiple web servers found ("+webserverNames.size()+")");
		}
	}
}
