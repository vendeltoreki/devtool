package fixenv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fixenv.util.FileUtil;

public class FixSetenv {
	private String bundleRoot;
	private List<String> sourceValues = new ArrayList<>();
	private List<String> targetValues = new ArrayList<>();
	
	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void execute() {
		String filePath = FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/bin/setenv.bat");
		
		System.out.println("Updating values in file: "+filePath);

		readSetenvFile(filePath);
		updateSetenvFile(filePath);
	}

	/*private void readPropertiesFile(String propertiesFilePath) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			File propertiesFile = new File(propertiesFilePath);
			if (propertiesFile.exists() && propertiesFile.isFile()) { 
				input = new FileInputStream(propertiesFilePath);
				
				prop.load(input);
				
				bundleEntry.setDbDriverClass(prop.getProperty("jdbc.default.driverClassName"));
				bundleEntry.setDbUrl(prop.getProperty("jdbc.default.url"));
				bundleEntry.setDbUsername(prop.getProperty("jdbc.default.username"));
				bundleEntry.setDbPassword(prop.getProperty("jdbc.default.password"));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}*/

	public void info() {
		String filePath = FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/bin/setenv.bat");
		
		System.out.println("\n-- Memory settings ----------------------------------");
		System.out.println("Values in file: "+filePath);
		
		readSetenvFile(filePath);
		
		if (sourceValues.size() > 0 && targetValues.size() == sourceValues.size()) {
			for (int i=0; i < sourceValues.size(); ++i) {
				String sourceValue = sourceValues.get(i);
				String targetValue = targetValues.get(i);
				
				System.out.println("Replace: \""+sourceValue+"\" with \""+targetValue+"\"");
			}
		}
	}
	
	private void readSetenvFile(String filePath) {
		try {
			List<String> lines = Files.readAllLines(new File(filePath).toPath());
			processSetenvFile(lines);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void updateSetenvFile(String filePath) {
		if (sourceValues.size() > 0 && targetValues.size() == sourceValues.size()) {
			replaceStringsInFile(filePath, sourceValues, targetValues);
		}
	}
	
	private String[][] memSettings = new String[][] {
		{"-Xmx", "4g"},
		{"-Xms", "4g"},
		{"-XX:MaxPermSize=", "512m"},
		{"-XX:MaxNewSize=", "512m"},
		{"-XX:NewSize=", "512m"},
		{"-XX:MaxMetaspaceSize=", "512m"},
		{"-XX:MetaspaceSize=", "512m"},
	};
	
	private void processSetenvFile(List<String> lines) {
		sourceValues.clear();
		targetValues.clear();

		for (String line : lines) {
			if (line.contains("CATALINA_OPTS")) {
				for (String[] memSetting : memSettings) {
					String prefix = memSetting[0];
					String pattern = prefix+"([0-9]+[mg])";
					String value = tryParseFirstMatch(line, pattern);
					
					if (value != null) {
						//System.out.println("Found \""+pattern+"\": "+value);
						
						int actual = getIntValue(value);
						int reference = getIntValue(memSetting[1]);
						
						//System.out.println("actual: "+actual+", ref: "+reference);
						
						if (actual < reference) {
							sourceValues.add(prefix+value);
							targetValues.add(prefix+memSetting[1]);
							
							//replacedLine = replacedLine.replaceFirst(pattern, prefix+memSetting[1]);
						}
					}
					
				}
			}
		}
	}

	private int getIntValue(String stringValue) {
		try {
			int intValue = Integer.valueOf(stringValue.substring(0, stringValue.length()-1));
			
			if (stringValue.endsWith("g")) {
				intValue = intValue * 1024;
			}
			
			return intValue;
		} catch (Exception ex) {
			
		}
		
		return 0;
	}
	
	private String tryParseFirstMatch(String line, String pattern) {
		String res = null;

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(line);
		if (m.find()) {
			res = m.group(1);
			// System.out.println("MATCH: "+res);
		}

		return res;
	}

	private void replaceStringsInFile(String filePath, List<String> sourceValues, List<String> targetValues) {
		try {
			Path path = Paths.get(filePath);

			String content = new String(Files.readAllBytes(path));
			
			for (int i=0; i < sourceValues.size(); ++i) {
				String sourceValue = sourceValues.get(i);
				String targetValue = targetValues.get(i);
				
				System.out.println("Replace: \""+sourceValue+"\" with \""+targetValue+"\"");
				
				content = content.replaceAll(sourceValue, targetValue);
			}
			
			Files.write(path, content.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
 }
